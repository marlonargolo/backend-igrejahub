package com.igrejahub.admin.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.tenant.TenantContext;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    // ── Painel / estatísticas ─────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Long orgId = TenantContext.getCurrentTenant();
        long churches     = q1("SELECT COUNT(*) FROM churches WHERE organization_id=? AND (deleted IS NULL OR deleted=false)", orgId);
        long congregations= q1("SELECT COUNT(*) FROM congregations WHERE organization_id=? AND (deleted IS NULL OR deleted=false)", orgId);
        long users        = q1("SELECT COUNT(*) FROM users WHERE organization_id=? AND (deleted IS NULL OR deleted=false)", orgId);
        long members      = q1("SELECT COUNT(*) FROM members WHERE organization_id=? AND (deleted IS NULL OR deleted=false)", orgId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "churches", churches, "congregations", congregations,
            "users", users, "members", members)));
    }

    // ── Igrejas ───────────────────────────────────────────────────────────────

    @GetMapping("/churches")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getChurches() {
        Long orgId = TenantContext.getCurrentTenant();
        var list = jdbcTemplate.queryForList(
            "SELECT c.id, c.name, c.city, c.state, c.cnpj, c.status, c.logo_url, " +
            "  p.name AS plan_name, " +
            "  (SELECT COUNT(*) FROM congregations cg WHERE cg.church_id=c.id AND (cg.deleted IS NULL OR cg.deleted=false)) AS congregation_count, " +
            "  (SELECT COUNT(*) FROM users u WHERE u.church_id=c.id AND (u.deleted IS NULL OR u.deleted=false)) AS user_count, " +
            "  (SELECT COUNT(*) FROM members m WHERE m.church_id=c.id AND (m.deleted IS NULL OR m.deleted=false)) AS member_count " +
            "FROM churches c " +
            "LEFT JOIN plans p ON c.plan_id=p.id " +
            "WHERE c.organization_id=? AND (c.deleted IS NULL OR c.deleted=false) " +
            "ORDER BY c.name", orgId);
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
        long congs   = q1("SELECT COUNT(*) FROM congregations WHERE organization_id=? AND church_id=? AND (deleted IS NULL OR deleted=false)", orgId, id);
        long users   = q1("SELECT COUNT(*) FROM users WHERE organization_id=? AND church_id=? AND (deleted IS NULL OR deleted=false)", orgId, id);
        long members = q1("SELECT COUNT(*) FROM members WHERE organization_id=? AND church_id=? AND (deleted IS NULL OR deleted=false)", orgId, id);
        long docs    = q1("SELECT COUNT(*) FROM documents WHERE organization_id=? AND church_id=? AND (deleted IS NULL OR deleted=false)", orgId, id);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "congregations", congs, "users", users, "members", members, "documents", docs)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private long q1(String sql, Object... args) {
        Long v = jdbcTemplate.queryForObject(sql, Long.class, args);
        return v != null ? v : 0L;
    }
}