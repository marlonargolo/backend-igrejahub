package com.igrejahub.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component("permissionEvaluator")
public class PermissionEvaluator implements org.springframework.security.access.PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return checkPermission(authentication, permission);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return checkPermission(authentication, permission);
    }

    private boolean checkPermission(Authentication authentication, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (!(authentication.getPrincipal() instanceof com.igrejahub.security.UserPrincipal user)) {
            return false;
        }
        String required = String.valueOf(permission);
        return user.hasPermission("ROOT_ACCESS") || user.hasPermission(required);
    }
}