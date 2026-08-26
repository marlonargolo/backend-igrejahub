package com.igrejahub.finance.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.finance.dto.*;
import com.igrejahub.finance.service.FinancialTransactionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/finance/transactions")
@Tag(name = "Finance Transactions", description = "Transações financeiras")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class FinancialTransactionController {

    private final FinancialTransactionService transactionService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
    public ResponseEntity<ApiResponse<Page<FinancialTransactionDto>>> getTransactions(
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String status) {
        FinancialFilterDto filter = FinancialFilterDto.builder().status(status).build();
        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransactions(pageable, filter)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
    public ResponseEntity<ApiResponse<FinancialTransactionDto>> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransaction(id)));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'FINANCE_MANAGE')")
    public ResponseEntity<ApiResponse<FinancialTransactionDto>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.createTransaction(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'FINANCE_MANAGE')")
    public ResponseEntity<ApiResponse<FinancialTransactionDto>> updateTransaction(
            @PathVariable Long id, @Valid @RequestBody UpdateTransactionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.updateTransaction(id, request)));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasPermission(null, 'FINANCE_APPROVE')")
    public ResponseEntity<ApiResponse<FinancialTransactionDto>> confirmTransaction(
            @PathVariable Long id, @RequestBody(required = false) ConfirmTransactionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.confirmTransaction(id, request)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasPermission(null, 'FINANCE_APPROVE')")
    public ResponseEntity<ApiResponse<FinancialTransactionDto>> cancelTransaction(
            @PathVariable Long id, @Valid @RequestBody CancelTransactionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.cancelTransaction(id, request)));
    }

    @PostMapping(value = "/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        String ext = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = "tx_" + id + "_" + UUID.randomUUID() + ext;
        Path uploadDir = Paths.get("/app/uploads/attachments");
        Files.createDirectories(uploadDir);
        file.transferTo(uploadDir.resolve(fileName).toFile());
        String url = "/uploads/attachments/" + fileName;
        transactionService.updateAttachment(id, url);
        return ResponseEntity.ok(ApiResponse.success(Map.of("attachmentUrl", url)));
    }
}
