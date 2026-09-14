package com.igrejahub.admin.controller;

import com.igrejahub.admin.service.GlobalSettingsService;
import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.security.SecurityUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Administração Externa — exclusivo ROOT.
 * Todos os endpoints exigem ROOT_ACCESS.
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "Admin External")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
public class AdminController {

    private final JdbcTemplate jdbcTemplate;
    private final SecurityUtils securityUtils;
    private final GlobalSettingsService globalSettingsService;

    // ── Painel / estatísticas ─────────────────────────────────────────────────

    /**
     * Painel geral. ROOT em modo global → indicadores de toda a organização.
     * ROOT em modo filtrado (X-Church-Id) → indicadores só daquela Igreja.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Long orgId = TenantContext.getCurrentTenant();
        Long churchId = securityUtils.canViewAll() ? null : TenantContext.getCurrentChurchId();
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);

        Map<String, Object> stats = new HashMap<>();
        stats.put("churches", churchId != null ? 1L
            : q1("SELECT COUNT(*) FROM churches WHERE organization_id=? AND (deleted IS NULL OR deleted=false)", orgId));
        stats.put("congregations", q1(
            "SELECT COUNT(*) FROM congregations WHERE organization_id=? AND (deleted IS NULL OR deleted=false) " +
            "AND (?::bigint IS NULL OR church_id=?)", orgId, churchId, churchId));
        stats.put("users", q1(
            "SELECT COUNT(*) FROM users WHERE organization_id=? AND (deleted IS NULL OR deleted=false) " +
            "AND (?::bigint IS NULL OR church_id=?)", orgId, churchId, churchId));
        stats.put("usersByRole", jdbcTemplate.queryForList(
            "SELECT r.name AS role, COUNT(DISTINCT u.id) AS total FROM users u " +
            "JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id " +
            "WHERE u.organization_id=? AND (u.deleted IS NULL OR u.deleted=false) " +
            "AND (?::bigint IS NULL OR u.church_id=?) GROUP BY r.name ORDER BY r.name",
            orgId, churchId, churchId));
        stats.put("activeMembers", q1(
            "SELECT COUNT(*) FROM members WHERE organization_id=? AND status='ACTIVE' AND (deleted IS NULL OR deleted=false) " +
            "AND (?::bigint IS NULL OR church_id=?)", orgId, churchId, churchId));
        stats.put("transactionsThisPeriod", q1(
            "SELECT COUNT(*) FROM financial_transactions WHERE organization_id=? AND transaction_date>=? " +
            "AND (deleted IS NULL OR deleted=false) AND (?::bigint IS NULL OR church_id=?)",
            orgId, monthStart, churchId, churchId));
        stats.put("consolidatedBalanceCents", q1(
            "SELECT COALESCE(SUM(current_balance_cents),0) FROM finance_accounts WHERE organization_id=? " +
            "AND active=true AND (deleted IS NULL OR deleted=false) AND (?::bigint IS NULL OR church_id=?)",
            orgId, churchId, churchId));
        stats.put("documents", q1(
            "SELECT COUNT(*) FROM documents WHERE organization_id=? AND (deleted IS NULL OR deleted=false) " +
            "AND (?::bigint IS NULL OR church_id=?)", orgId, churchId, churchId));
        stats.put("openSupportTickets", q1(
            "SELECT COUNT(*) FROM support_tickets WHERE organization_id=? AND status NOT IN ('FECHADO','RESOLVIDO') " +
            "AND (?::bigint IS NULL OR church_id=?)", orgId, churchId, churchId));
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ── Igrejas ───────────────────────────────────────────────────────────────

    @GetMapping("/churches")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getChurches(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate createdTo) {
        Long orgId = TenantContext.getCurrentTenant();
        var list = jdbcTemplate.queryForList(
            "SELECT c.id, c.name, c.city, c.state, c.cnpj, c.status, c.logo_url, c.created_at, " +
            "  p.name AS plan_name, " +
            "  (SELECT COUNT(*) FROM congregations cg WHERE cg.church_id=c.id AND (cg.deleted IS NULL OR cg.deleted=false)) AS congregation_count, " +
            "  (SELECT COUNT(*) FROM users u WHERE u.church_id=c.id AND (u.deleted IS NULL OR u.deleted=false)) AS user_count, " +
            "  (SELECT COUNT(*) FROM members m WHERE m.church_id=c.id AND (m.deleted IS NULL OR m.deleted=false)) AS member_count " +
            "FROM churches c " +
            "LEFT JOIN plans p ON c.plan_id=p.id " +
            "WHERE c.organization_id=? AND (c.deleted IS NULL OR c.deleted=false) " +
            "AND (?::text IS NULL OR c.name ILIKE '%' || ? || '%') " +
            "AND (?::text IS NULL OR c.city ILIKE '%' || ? || '%') " +
            "AND (?::text IS NULL OR c.state = ?) " +
            "AND (?::text IS NULL OR c.status = ?) " +
            "AND (?::bigint IS NULL OR c.plan_id = ?) " +
            "AND (?::date IS NULL OR c.created_at >= ?) " +
            "AND (?::date IS NULL OR c.created_at <= ?) " +
            "ORDER BY c.name",
            orgId, name, name, city, city, state, state, status, status,
            planId, planId, createdFrom, createdFrom, createdTo, createdTo);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/churches/{id}/congregations")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCongregations(@PathVariable Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        var list = jdbcTemplate.queryForList(
            "SELECT cg.id, cg.name, cg.city, cg.state, cg.status, " +
            "  (SELECT COUNT(*) FROM users u WHERE u.congregation_id=cg.id AND (u.deleted IS NULL OR u.deleted=false)) AS user_count, " +
            "  (SELECT COUNT(*) FROM members m WHERE m.congregation_id=cg.id AND (m.deleted IS NULL OR m.deleted=false)) AS member_count " +
            "FROM congregations cg " +
            "WHERE cg.organization_id=? AND cg.church_id=? AND (cg.deleted IS NULL OR cg.deleted=false) " +
            "ORDER BY cg.name", orgId, id);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/churches/{id}/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUsers(@PathVariable Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        var list = jdbcTemplate.queryForList(
            "SELECT u.id, u.name, u.email, u.is_active AS active, " +
            "  (SELECT STRING_AGG(r.name,', ') FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE ur.user_id=u.id) AS roles, " +
            "  u.created_at " +
            "FROM users u " +
            "WHERE u.organization_id=? AND u.church_id=? AND (u.deleted IS NULL OR u.deleted=false) " +
            "ORDER BY u.name", orgId, id);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/churches/{id}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChurchStats(@PathVariable Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        long congs      = q1("SELECT COUNT(*) FROM congregations WHERE organization_id=? AND church_id=? AND (deleted IS NULL OR deleted=false)", orgId, id);
        long users      = q1("SELECT COUNT(*) FROM users WHERE organization_id=? AND church_id=? AND (deleted IS NULL OR deleted=false)", orgId, id);
        long members    = q1("SELECT COUNT(*) FROM members WHERE organization_id=? AND church_id=? AND (deleted IS NULL OR deleted=false)", orgId, id);
        long activeMembers = q1("SELECT COUNT(*) FROM members WHERE organization_id=? AND church_id=? AND status='ACTIVE' AND (deleted IS NULL OR deleted=false)", orgId, id);
        long docs       = q1("SELECT COUNT(*) FROM documents WHERE organization_id=? AND church_id=? AND (deleted IS NULL OR deleted=false)", orgId, id);
        long transactionsThisMonth = q1(
            "SELECT COUNT(*) FROM financial_transactions WHERE organization_id=? AND church_id=? " +
            "AND transaction_date>=? AND (deleted IS NULL OR deleted=false)",
            orgId, id, LocalDate.now().withDayOfMonth(1));
        long balanceCents = q1(
            "SELECT COALESCE(SUM(current_balance_cents),0) FROM finance_accounts WHERE organization_id=? " +
            "AND church_id=? AND active=true AND (deleted IS NULL OR deleted=false)", orgId, id);
        long openTickets = q1(
            "SELECT COUNT(*) FROM support_tickets WHERE organization_id=? AND church_id=? " +
            "AND status NOT IN ('FECHADO','RESOLVIDO')", orgId, id);
        Map<String, Object> stats = new HashMap<>();
        stats.put("congregations", congs);
        stats.put("users", users);
        stats.put("members", members);
        stats.put("activeMembers", activeMembers);
        stats.put("documents", docs);
        stats.put("transactionsThisPeriod", transactionsThisMonth);
        stats.put("consolidatedBalanceCents", balanceCents);
        stats.put("openSupportTickets", openTickets);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ── Configurações globais ────────────────────────────────────────────────

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(globalSettingsService.getSettings()));
    }

    @PutMapping("/settings/{key}")
    public ResponseEntity<ApiResponse<Object>> updateSetting(
            @PathVariable String key, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(globalSettingsService.updateSetting(key, body.get("value"))));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private long q1(String sql, Object... args) {
        Long v = jdbcTemplate.queryForObject(sql, Long.class, args);
        return v != null ? v : 0L;
    }
}