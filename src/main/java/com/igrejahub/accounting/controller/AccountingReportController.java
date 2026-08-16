package com.igrejahub.accounting.controller;

import com.igrejahub.accounting.service.AccountingPeriodService;
import com.igrejahub.accounting.service.AccountingReportService;
import com.igrejahub.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accounting/reports")
@Tag(name = "Accounting Reports", description = "Relatórios contábeis")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class AccountingReportController {

    private final AccountingReportService reportService;
    private final AccountingPeriodService periodService;

    @GetMapping("/trial-balance")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_VIEW')")
    public ResponseEntity<ApiResponse<List<AccountingReportService.TrialBalanceLine>>> getTrialBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getTrialBalance(startDate, endDate)));
    }

    @GetMapping("/periods")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_VIEW')")
    public ResponseEntity<ApiResponse<?>> getPeriods(
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(periodService.getPeriods(pageable)));
    }

    @PostMapping("/periods")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_MANAGE')")
    public ResponseEntity<ApiResponse<?>> createPeriod(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(periodService.createPeriod(
                body.get("name"), LocalDate.parse(body.get("startDate")), LocalDate.parse(body.get("endDate")))));
    }

    @PatchMapping("/periods/{id}/close")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_CLOSE_PERIOD')")
    public ResponseEntity<ApiResponse<?>> closePeriod(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(periodService.closePeriod(id)));
    }

    @PatchMapping("/periods/{id}/reopen")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_CLOSE_PERIOD')")
    public ResponseEntity<ApiResponse<?>> reopenPeriod(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(periodService.reopenPeriod(id)));
    }
}