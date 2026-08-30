package com.igrejahub.common.tenant;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TenantContext {

    private static final ThreadLocal<Long>   currentTenant        = new ThreadLocal<>();
    private static final ThreadLocal<Long>   currentUserId        = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUserEmail     = new ThreadLocal<>();
    private static final ThreadLocal<Long>   currentChurchId      = new ThreadLocal<>();
    private static final ThreadLocal<Long>   currentCongregationId = new ThreadLocal<>();

    private TenantContext() {}

    // Organization
    public static void setCurrentTenant(Long organizationId) {
        log.debug("TenantContext org={}", organizationId);
        currentTenant.set(organizationId);
    }
    public static Long getCurrentTenant() { return currentTenant.get(); }
    public static boolean isTenantSet()   { return currentTenant.get() != null; }

    // User
    public static void setCurrentUserId(Long userId)     { currentUserId.set(userId); }
    public static Long getCurrentUserId()                { return currentUserId.get(); }
    public static void setCurrentUserEmail(String email) { currentUserEmail.set(email); }
    public static String getCurrentUserEmail()           { return currentUserEmail.get(); }

    // Church
    public static void setCurrentChurchId(Long churchId) {
        log.debug("TenantContext church={}", churchId);
        currentChurchId.set(churchId);
    }
    public static Long getCurrentChurchId() { return currentChurchId.get(); }

    // Congregation
    public static void setCurrentCongregationId(Long congregationId) {
        log.debug("TenantContext congregation={}", congregationId);
        currentCongregationId.set(congregationId);
    }
    public static Long getCurrentCongregationId()  { return currentCongregationId.get(); }
    public static boolean isCongregationScoped()   { return currentCongregationId.get() != null; }

    public static void clear() {
        currentTenant.remove();
        currentUserId.remove();
        currentUserEmail.remove();
        currentChurchId.remove();
        currentCongregationId.remove();
    }
}