package com.igrejahub.security;

import com.igrejahub.common.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class SecurityUtils {

    public Optional<UserPrincipal> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal) {
            return Optional.of((UserPrincipal) principal);
        }
        return Optional.empty();
    }

    public Long getCurrentOrganizationId() {
        return TenantContext.getCurrentTenant();
    }

    public Long getCurrentUserId() {
        return TenantContext.getCurrentUserId();
    }

    public String getCurrentUserEmail() {
        return TenantContext.getCurrentUserEmail();
    }

    public boolean hasPermission(String permission) {
        return getCurrentUser()
            .map(user -> user.hasPermission(permission))
            .orElse(false);
    }

    public boolean isRoot() {
        return getCurrentUser()
            .map(user -> user.hasPermission("ROOT_ACCESS"))
            .orElse(false);
    }

    public boolean isAdmin() {
        return getCurrentUser()
            .map(user -> user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
            .orElse(false);
    }
}
