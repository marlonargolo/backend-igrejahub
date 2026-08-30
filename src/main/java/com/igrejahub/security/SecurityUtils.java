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
        if (auth.getPrincipal() instanceof UserPrincipal user) return Optional.of(user);
        return Optional.empty();
    }

    public Long getCurrentUserId()         { return TenantContext.getCurrentUserId(); }
    public String getCurrentUserEmail()    { return TenantContext.getCurrentUserEmail(); }
    public Long getCurrentOrganizationId() { return TenantContext.getCurrentTenant(); }
    public Long getCurrentChurchId()       { return TenantContext.getCurrentChurchId(); }
    public Long getCurrentCongregationId() { return TenantContext.getCurrentCongregationId(); }
    public boolean isCongregationScoped()  { return TenantContext.isCongregationScoped(); }

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

    public boolean canAccessChurch(Long resourceChurchId) {
        if (isRoot()) return true;
        Long userChurchId = getCurrentChurchId();
        return userChurchId != null && userChurchId.equals(resourceChurchId);
    }

    public boolean canAccessCongregation(Long resourceChurchId, Long resourceCongregationId) {
        if (isRoot()) return true;
        if (!canAccessChurch(resourceChurchId)) return false;
        Long userCongId = getCurrentCongregationId();
        if (userCongId == null) return true;
        return userCongId.equals(resourceCongregationId);
    }
}