package com.igrejahub.security.filter;

import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Popula o TenantContext a partir do usuário autenticado e dos headers do request.
 *
 * Headers reconhecidos:
 *   X-Root-Mode : "global"   → ROOT vê tudo da organização (sem filtro de Igreja)
 *   X-Root-Mode : "filtered" → ROOT está com uma Igreja selecionada (usa X-Church-Id)
 *   X-Church-Id : <id>       → ID da Igreja selecionada pelo ROOT na tela de seleção
 *
 * Lógica de churchId por tipo de usuário:
 *   ROOT + X-Root-Mode=global   → churchId = null (vê tudo)
 *   ROOT + X-Root-Mode=filtered → churchId = X-Church-Id header
 *   ROOT sem header              → churchId = null (modo global por padrão)
 *   Não-ROOT                    → churchId = user.getChurchId() do JWT (fixo)
 *                                  congregationId = user.getCongregationId() do JWT
 */
@Slf4j
@Component
public class TenantIsolationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof UserPrincipal user) {

            // Campos sempre presentes
            TenantContext.setCurrentTenant(user.getOrganizationId());
            TenantContext.setCurrentUserId(user.getId());
            TenantContext.setCurrentUserEmail(user.getEmail());

            boolean isRoot = user.hasPermission("ROOT_ACCESS");

            if (isRoot) {
                // ROOT: churchId vem do header X-Church-Id, não do JWT
                String rootMode    = request.getHeader("X-Root-Mode");
                String churchIdHdr = request.getHeader("X-Church-Id");

                boolean globalMode = !"filtered".equalsIgnoreCase(rootMode);

                if (globalMode) {
                    // ROOT no modo global: vê tudo, sem filtro de Igreja
                    TenantContext.setCurrentChurchId(null);
                    TenantContext.setCurrentCongregationId(null);
                    TenantContext.setRootGlobalMode(true);
                    log.debug("ROOT global mode — org={} path={}",
                        user.getOrganizationId(), request.getRequestURI());
                } else {
                    // ROOT modo filtrado: usa X-Church-Id do header
                    Long churchId = null;
                    if (churchIdHdr != null && !churchIdHdr.isBlank()) {
                        try {
                            churchId = Long.parseLong(churchIdHdr.trim());
                        } catch (NumberFormatException e) {
                            log.warn("X-Church-Id inválido: '{}' — tratando como modo global", churchIdHdr);
                        }
                    }
                    TenantContext.setCurrentChurchId(churchId);
                    TenantContext.setCurrentCongregationId(null); // ROOT não tem congregação
                    TenantContext.setRootGlobalMode(churchId == null);
                    log.debug("ROOT filtered mode — org={} church={} path={}",
                        user.getOrganizationId(), churchId, request.getRequestURI());
                }
            } else {
                // Não-ROOT: churchId e congregationId vêm do JWT (fixos no cadastro)
                TenantContext.setCurrentChurchId(user.getChurchId());
                TenantContext.setCurrentCongregationId(user.getCongregationId());
                TenantContext.setRootGlobalMode(false);
                log.debug("User mode — org={} church={} congregation={} path={}",
                    user.getOrganizationId(), user.getChurchId(),
                    user.getCongregationId(), request.getRequestURI());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
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