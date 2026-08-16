package com.igrejahub.common.tenant;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TenantContext {
    
    private static final ThreadLocal<Long> currentTenant = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUserEmail = new ThreadLocal<>();
    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();

    private TenantContext() {}

    public static void setCurrentTenant(Long organizationId) {
        log.debug("Setting tenant context: {}", organizationId);
        currentTenant.set(organizationId);
    }

    public static Long getCurrentTenant() {
        return currentTenant.get();
    }

    public static void setCurrentUserEmail(String email) {
        currentUserEmail.set(email);
    }

    public static String getCurrentUserEmail() {
        return currentUserEmail.get();
    }

    public static void setCurrentUserId(Long userId) {
        currentUserId.set(userId);
    }

    public static Long getCurrentUserId() {
        return currentUserId.get();
    }

    public static void clear() {
        currentTenant.remove();
        currentUserEmail.remove();
        currentUserId.remove();
    }

    public static boolean isTenantSet() {
        return currentTenant.get() != null;
    }
}
