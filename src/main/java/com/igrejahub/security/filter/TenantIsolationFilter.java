package com.igrejahub.security.filter;

import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Popula TenantContext a cada request autenticado.
 * Agora que UserPrincipal tem churchId e congregationId, usa-os diretamente.
 * ROOT: churchId vem do header X-Church-Id (não do JWT, pois ROOT não tem Igreja fixa).
 * Não-ROOT: churchId vem do UserPrincipal (gravado no JWT via JwtService).
 *
 * Admin de Igreja (churchId fixo, sem congregationId fixo) pode "entrar" numa
 * congregação específica da própria Igreja via header X-Congregation-Id —
 * mesmo mecanismo do ROOT com X-Church-Id, só que um nível abaixo. O valor é
 * sempre validado contra a Igreja do usuário antes de ser aplicado; nunca é
 * aceito para usuários já fixos numa congregação (JWT sempre prevalece).
 */
@Component
@RequiredArgsConstructor
public class TenantIsolationFilter extends OncePerRequestFilter {

    private final CongregationRepository congregationRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof UserPrincipal user) {

            TenantContext.setCurrentTenant(user.getOrganizationId());
            TenantContext.setCurrentUserId(user.getId());
            TenantContext.setCurrentUserEmail(user.getEmail());

            if (user.hasPermission("ROOT_ACCESS")) {
                String rootMode    = request.getHeader("X-Root-Mode");
                String churchIdHdr = request.getHeader("X-Church-Id");
                boolean global     = !"filtered".equalsIgnoreCase(rootMode);

                Long resolvedChurchId = null;
                if (!global && churchIdHdr != null && !churchIdHdr.isBlank()) {
                    try {
                        resolvedChurchId = Long.parseLong(churchIdHdr.trim());
                        TenantContext.setCurrentChurchId(resolvedChurchId);
                        TenantContext.setRootGlobalMode(false);
                    } catch (NumberFormatException e) {
                        TenantContext.setCurrentChurchId(null);
                        TenantContext.setRootGlobalMode(true);
                    }
                } else {
                    TenantContext.setCurrentChurchId(null);
                    TenantContext.setRootGlobalMode(true);
                }

                // ROOT também pode "entrar" numa Congregação específica da Igreja
                // que está gerenciando no momento (mesmo mecanismo do admin de
                // Igreja, um nível acima). Sem isso, todo módulo (Membros,
                // Patrimônio, etc.) continuava filtrando só por churchId e
                // devolvia os dados da Igreja inteira dentro da Congregação.
                TenantContext.setCurrentCongregationId(
                    resolveEnteredCongregationId(request, user.getOrganizationId(), resolvedChurchId));

            } else {
                // Não-ROOT: usa os campos do UserPrincipal (que agora existem)
                TenantContext.setCurrentChurchId(user.getChurchId());
                TenantContext.setRootGlobalMode(false);

                if (user.getCongregationId() != null) {
                    // Usuário fixo numa Congregação: JWT sempre prevalece, nunca
                    // pode ser sobrescrito por header.
                    TenantContext.setCurrentCongregationId(user.getCongregationId());
                } else {
                    TenantContext.setCurrentCongregationId(
                        resolveEnteredCongregationId(request, user.getOrganizationId(), user.getChurchId()));
                }
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * "Entrar" numa Congregação específica via header — usado tanto pelo admin
     * de Igreja (churchId fixo no JWT) quanto pelo ROOT (churchId resolvido a
     * partir do header X-Church-Id). Só é aplicado se a Congregação pertencer
     * à própria Igreja em questão — caso contrário, ou se o header estiver
     * ausente/inválido, ou não houver Igreja definida, retorna null (visão da
     * Igreja toda, comportamento padrão de sempre).
     */
    private Long resolveEnteredCongregationId(HttpServletRequest request, Long organizationId, Long churchId) {
        String header = request.getHeader("X-Congregation-Id");
        if (header == null || header.isBlank() || churchId == null) {
            return null;
        }
        try {
            Long requestedCongId = Long.parseLong(header.trim());
            boolean belongsToChurch = congregationRepository.existsByOrganizationIdAndIdAndChurchId(
                organizationId, requestedCongId, churchId);
            return belongsToChurch ? requestedCongId : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/actuator/health");
    }
}