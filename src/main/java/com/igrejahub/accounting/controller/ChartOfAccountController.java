package com.igrejahub.accounting.controller;

import com.igrejahub.accounting.dto.ChartOfAccountDto;
import com.igrejahub.accounting.service.ChartOfAccountService;
import com.igrejahub.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accounting/chart-of-accounts")
@Tag(name = "Chart of Accounts", description = "Plano de contas")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class ChartOfAccountController {

    private final ChartOfAccountService accountService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_VIEW')")
    public ResponseEntity<ApiResponse<List<ChartOfAccountDto>>> getAccounts() {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccounts()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_VIEW')")
    public ResponseEntity<ApiResponse<ChartOfAccountDto>> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccount(id)));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_MANAGE')")
    public ResponseEntity<ApiResponse<ChartOfAccountDto>> createAccount(@RequestBody Map<String, Object> body) {
        var dto = accountService.createAccount(
                (String) body.get("code"),
                (String) body.get("name"),
                (String) body.get("accountType"),
                body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null,
                (String) body.get("normalBalance"),
                body.get("analytical") == null || (Boolean) body.get("analytical"));
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_MANAGE')")
    public ResponseEntity<ApiResponse<ChartOfAccountDto>> updateAccount(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean active = body.get("active") != null ? (Boolean) body.get("active") : null;
        return ResponseEntity.ok(ApiResponse.success(
                accountService.updateAccount(id, (String) body.get("name"), active)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}