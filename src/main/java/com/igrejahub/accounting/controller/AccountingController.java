package com.igrejahub.accounting.controller;

import com.igrejahub.accounting.service.AccountingService;
import com.igrejahub.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounting")
@Tag(name = "Accounting", description = "Visão geral do módulo contábil")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class AccountingController {

    private final AccountingService accountingService;

    @GetMapping("/overview")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_VIEW')")
    public ResponseEntity<ApiResponse<AccountingService.AccountingOverview>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(accountingService.getOverview()));
    }
}