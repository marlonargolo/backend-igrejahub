package com.igrejahub.finance.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.finance.dto.FinancialTransferDto;
import com.igrejahub.finance.service.FinancialTransferService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/finance/transfers")
@Tag(name = "Finance Transfers", description = "Transferências entre contas")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class FinancialTransferController {

    private final FinancialTransferService transferService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
    public ResponseEntity<ApiResponse<Page<FinancialTransferDto>>> getTransfers(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(transferService.getTransfers(pageable)));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'FINANCE_MANAGE')")
    public ResponseEntity<ApiResponse<FinancialTransferDto>> createTransfer(@RequestBody Map<String, Object> body) {
        var dto = transferService.createTransfer(
                Long.valueOf(body.get("fromAccountId").toString()),
                Long.valueOf(body.get("toAccountId").toString()),
                new BigDecimal(body.get("amount").toString()),
                body.get("transferDate") != null ? LocalDate.parse(body.get("transferDate").toString()) : null,
                (String) body.get("description"));
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}