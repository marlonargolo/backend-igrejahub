package com.igrejahub.finance.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.finance.dto.FinancialAccountDto;
import com.igrejahub.finance.service.FinancialAccountService;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/finance/accounts")
@Tag(name = "Finance Accounts", description = "Contas financeiras")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class FinancialAccountController {

    private final FinancialAccountService accountService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
    public ResponseEntity<ApiResponse<Page<FinancialAccountDto>>> getAccounts(@PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccounts(pageable)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
    public ResponseEntity<ApiResponse<List<FinancialAccountDto>>> getActiveAccounts() {
        return ResponseEntity.ok(ApiResponse.success(accountService.getActiveAccounts()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
    public ResponseEntity<ApiResponse<FinancialAccountDto>> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccount(id)));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'FINANCE_MANAGE')")
    public ResponseEntity<ApiResponse<FinancialAccountDto>> createAccount(@RequestBody Map<String, Object> body) {
        var dto = accountService.createAccount(
                (String) body.get("name"),
                (String) body.get("type"),
                (String) body.get("bankName"),
                (String) body.get("agency"),
                (String) body.get("accountNumber"),
                body.get("initialBalance") != null ? new BigDecimal(body.get("initialBalance").toString()) : BigDecimal.ZERO,
                body.get("churchId") != null ? Long.valueOf(body.get("churchId").toString()) : null);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'FINANCE_MANAGE')")
    public ResponseEntity<ApiResponse<FinancialAccountDto>> updateAccount(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean active = body.get("active") != null ? (Boolean) body.get("active") : null;
        var dto = accountService.updateAccount(id,
                (String) body.get("name"), (String) body.get("bankName"),
                (String) body.get("agency"), (String) body.get("accountNumber"), active);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'FINANCE_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}