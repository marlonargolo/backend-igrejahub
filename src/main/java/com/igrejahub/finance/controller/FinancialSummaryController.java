package com.igrejahub.finance.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.finance.dto.FinancialSummaryDto;
import com.igrejahub.finance.service.FinancialSummaryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/finance/summary")
@Tag(name = "Finance Summary", description = "Resumo financeiro")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class FinancialSummaryController {

    private final FinancialSummaryService summaryService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
    public ResponseEntity<ApiResponse<FinancialSummaryDto>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(summaryService.getSummary(startDate, endDate)));
    }
}