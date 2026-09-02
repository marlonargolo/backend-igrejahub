package com.igrejahub.security;

import com.igrejahub.common.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class SecurityUtils {

    public Optional<UserPrincipal> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        if (auth.getPrincipal() instanceof UserPrincipal u) return Optional.of(u);
        return Optional.empty();
    }

    // ── Identidade ────────────────────────────────────────────────────────────
    public Long getCurrentUserId()         { return TenantContext.getCurrentUserId(); }
    public String getCurrentUserEmail()    { return TenantContext.getCurrentUserEmail(); }
    public Long getCurrentOrganizationId() { return TenantContext.getCurrentTenant(); }
    public Long getCurrentChurchId()       { return TenantContext.getCurrentChurchId(); }
    public Long getCurrentCongregationId() { return TenantContext.getCurrentCongregationId(); }

    // ── Papéis ────────────────────────────────────────────────────────────────
    public boolean isRoot() {
        return getCurrentUser().map(u -> u.hasPermission("ROOT_ACCESS")).orElse(false);
    }
    public boolean isAdmin() {
        return getCurrentUser()
            .map(u -> u.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
            .orElse(false);
    }
    public boolean hasPermission(String permission) {
        return getCurrentUser().map(u -> u.hasPermission(permission)).orElse(false);
    }

    // ── Contexto efetivo ──────────────────────────────────────────────────────

    /**
     * ROOT no modo global (sem Igreja selecionada) → retorna null = "ver tudo".
     * ROOT com Igreja selecionada → retorna o churchId da Igreja.
     * Não-ROOT → retorna o churchId do usuário (obrigatório).
     */
    public Long getEffectiveChurchId() {
        if (isRoot() && TenantContext.isRootGlobalMode()) {
            return null; // ROOT vê tudo
        }
        return TenantContext.getCurrentChurchId();
    }

    /**
     * true somente quando ROOT está no modo "Todas as Igrejas".
     * Usado para queries sem filtro de church_id.
     */
    public boolean canViewAll() {
        return isRoot() && TenantContext.isRootGlobalMode();
    }

    // ── Validações de acesso ──────────────────────────────────────────────────

    public boolean canAccessChurch(Long resourceChurchId) {
        if (canViewAll()) return true;
        Long eff = getEffectiveChurchId();
        return eff != null && eff.equals(resourceChurchId);
    }

    public boolean canAccessCongregation(Long resourceChurchId, Long resourceCongregationId) {
        if (!canAccessChurch(resourceChurchId)) return false;
        Long congId = TenantContext.getCurrentCongregationId();
        if (congId == null) return true; // acesso à Igreja toda
        return congId.equals(resourceCongregationId);
    }
}