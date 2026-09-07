package com.igrejahub.accounting.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Observações do contador vinculadas a lançamentos financeiros.
 *
 * GET  /accounting/observations/{transactionId} — FINANCE_VIEW  (tesoureiro lê, contador lê)
 * POST /accounting/observations/{transactionId} — ACCOUNTING_UPDATE (só contador cria)
 * PUT  /accounting/observations/{id}            — ACCOUNTING_UPDATE
 * DEL  /accounting/observations/{id}            — ACCOUNTING_UPDATE
 *
 * Endpoint de conciliação (confirmar que o lançamento está revisado pelo contador):
 * PATCH /accounting/transactions/{id}/reconcile   — ACCOUNTING_UPDATE
 * PATCH /accounting/transactions/{id}/unreconcile — ACCOUNTING_UPDATE
 */
@Slf4j
@RestController
@RequestMapping("/accounting")
@RequiredArgsConstructor
public class AccountingObservationController {

    private final JdbcTemplate jdbcTemplate;
    private final SecurityUtils securityUtils;

    // ── Observações ───────────────────────────────────────────────────────────

    @GetMapping("/observations/{transactionId}")
    @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getObservations(
            @PathVariable Long transactionId) {
        Long orgId = TenantContext.getCurrentTenant();
        List<Map<String, Object>> obs = jdbcTemplate.queryForList(
            "SELECT o.id, o.transaction_id, o.observation, o.created_at, o.updated_at, " +
            "u.name AS created_by_name " +
            "FROM accounting_observations o " +
            "LEFT JOIN users u ON u.id = o.created_by " +
            "WHERE o.organization_id = ? AND o.transaction_id = ? " +
            "ORDER BY o.created_at ASC",
            orgId, transactionId);
        return ResponseEntity.ok(ApiResponse.success(obs));
    }

    @PostMapping("/observations/{transactionId}")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_UPDATE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createObservation(
            @PathVariable Long transactionId,
            @RequestBody Map<String, String> body) {
        Long orgId  = TenantContext.getCurrentTenant();
        Long userId = securityUtils.getCurrentUserId();
        String obs  = body.get("observation");

        jdbcTemplate.update(
            "INSERT INTO accounting_observations " +
            "(organization_id, transaction_id, observation, created_by, created_at) " +
            "VALUES (?, ?, ?, ?, ?)",
            orgId, transactionId, obs, userId, LocalDateTime.now());

        Long newId = jdbcTemplate.queryForObject(
            "SELECT id FROM accounting_observations WHERE organization_id = ? AND transaction_id = ? " +
            "ORDER BY created_at DESC LIMIT 1", Long.class, orgId, transactionId);

        log.info("Observation created: tx={} by={}", transactionId, userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "id", newId, "transactionId", transactionId, "observation", obs)));
    }

    @PutMapping("/observations/{id}")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> updateObservation(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Long orgId  = TenantContext.getCurrentTenant();
        Long userId = securityUtils.getCurrentUserId();
        jdbcTemplate.update(
            "UPDATE accounting_observations SET observation = ?, updated_by = ?, updated_at = ? " +
            "WHERE id = ? AND organization_id = ?",
            body.get("observation"), userId, LocalDateTime.now(), id, orgId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/observations/{id}")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> deleteObservation(@PathVariable Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        jdbcTemplate.update(
            "DELETE FROM accounting_observations WHERE id = ? AND organization_id = ?", id, orgId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ── Conciliação ───────────────────────────────────────────────────────────

    @PatchMapping("/transactions/{id}/reconcile")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> reconcile(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        jdbcTemplate.update(
            "UPDATE financial_transactions SET reconciled = true, reconciled_at = ?, reconciled_by = ? " +
            "WHERE id = ?", LocalDateTime.now(), userId, id);
        log.info("Transaction {} reconciled by {}", id, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/transactions/{id}/unreconcile")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> unreconcile(@PathVariable Long id) {
        jdbcTemplate.update(
            "UPDATE financial_transactions SET reconciled = false, reconciled_at = NULL, reconciled_by = NULL " +
            "WHERE id = ?", id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ── Lançamentos visíveis para o contador (mesmo endpoint da tesouraria) ──
    // O FinancialTransactionController GET /finance/transactions já aplica
    // o filtro de churchId via TenantContext — o contador usa o mesmo endpoint.
    // Aqui apenas os endpoints exclusivos de contabilidade.
}