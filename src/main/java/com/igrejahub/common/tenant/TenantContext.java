package com.igrejahub.common.tenant;

import lombok.extern.slf4j.Slf4j;

/**
 * Contexto de tenant por thread (ThreadLocal).
 * Populado pelo TenantIsolationFilter a cada request autenticado.
 *
 * Hierarquia:
 *   organizationId  → SaaS (toda a instalação)
 *   churchId        → Igreja selecionada (null = ROOT no modo global)
 *   congregationId  → Congregação (null = acesso à Igreja toda)
 *   rootGlobalMode  → true quando ROOT não tem Igreja selecionada
 */
@Slf4j
public final class TenantContext {

    private static final ThreadLocal<Long>    currentTenant         = new ThreadLocal<>();
    private static final ThreadLocal<String>  currentUserEmail      = new ThreadLocal<>();
    private static final ThreadLocal<Long>    currentUserId         = new ThreadLocal<>();
    private static final ThreadLocal<Long>    currentChurchId       = new ThreadLocal<>();
    private static final ThreadLocal<Long>    currentCongregationId = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> rootGlobalMode        = new ThreadLocal<>();

    private TenantContext() {}

    // ── Organization ──────────────────────────────────────────────────────────
    public static void setCurrentTenant(Long organizationId) {
        log.debug("TenantContext org={}", organizationId);
        currentTenant.set(organizationId);
    }
    public static Long getCurrentTenant()  { return currentTenant.get(); }
    public static boolean isTenantSet()    { return currentTenant.get() != null; }

    // ── User ──────────────────────────────────────────────────────────────────
    public static void setCurrentUserId(Long userId)     { currentUserId.set(userId); }
    public static Long getCurrentUserId()                { return currentUserId.get(); }
    public static void setCurrentUserEmail(String email) { currentUserEmail.set(email); }
    public static String getCurrentUserEmail()           { return currentUserEmail.get(); }

    // ── Church ────────────────────────────────────────────────────────────────
    public static void setCurrentChurchId(Long churchId) {
        log.debug("TenantContext church={}", churchId);
        currentChurchId.set(churchId);
    }
    public static Long getCurrentChurchId() { return currentChurchId.get(); }

    // ── Congregation ──────────────────────────────────────────────────────────
    public static void setCurrentCongregationId(Long congregationId) {
        log.debug("TenantContext congregation={}", congregationId);
        currentCongregationId.set(congregationId);
    }
    public static Long getCurrentCongregationId()  { return currentCongregationId.get(); }
    public static boolean isCongregationScoped()   { return currentCongregationId.get() != null; }

    // ── ROOT global mode ──────────────────────────────────────────────────────
    /**
     * true = ROOT está no modo "Todas as Igrejas" (churchId == null).
     * false = ROOT está com uma Igreja específica selecionada.
     */
    public static void setRootGlobalMode(boolean global) { rootGlobalMode.set(global); }
    public static boolean isRootGlobalMode() {
        return Boolean.TRUE.equals(rootGlobalMode.get());
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────
    public static void clear() {
        currentTenant.remove();
        currentUserEmail.remove();
        currentUserId.remove();
        currentChurchId.remove();
        currentCongregationId.remove();
        rootGlobalMode.remove();
    }
}