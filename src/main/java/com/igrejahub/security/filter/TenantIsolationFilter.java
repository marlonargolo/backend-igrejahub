package com.igrejahub.security.filter;

import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 */
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

            TenantContext.setCurrentTenant(user.getOrganizationId());
            TenantContext.setCurrentUserId(user.getId());
            TenantContext.setCurrentUserEmail(user.getEmail());

            if (user.hasPermission("ROOT_ACCESS")) {
                String rootMode    = request.getHeader("X-Root-Mode");
                String churchIdHdr = request.getHeader("X-Church-Id");
                boolean global     = !"filtered".equalsIgnoreCase(rootMode);

                if (!global && churchIdHdr != null && !churchIdHdr.isBlank()) {
                    try {
                        TenantContext.setCurrentChurchId(Long.parseLong(churchIdHdr.trim()));
                        TenantContext.setRootGlobalMode(false);
                    } catch (NumberFormatException e) {
                        TenantContext.setCurrentChurchId(null);
                        TenantContext.setRootGlobalMode(true);
                    }
                } else {
                    TenantContext.setCurrentChurchId(null);
                    TenantContext.setRootGlobalMode(true);
                }
                TenantContext.setCurrentCongregationId(null);

            } else {
                // Não-ROOT: usa os campos do UserPrincipal (que agora existem)
                TenantContext.setCurrentChurchId(user.getChurchId());
                TenantContext.setCurrentCongregationId(user.getCongregationId());
                TenantContext.setRootGlobalMode(false);
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