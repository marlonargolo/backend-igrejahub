package com.igrejahub.accounting.controller;

import com.igrejahub.accounting.dto.AccountingExportDto;
import com.igrejahub.accounting.service.AccountingExportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounting/export")
@Tag(name = "Accounting Export", description = "Exportação de dados contábeis")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class AccountingExportController {

    private final AccountingExportService exportService;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'ACCOUNTING_VIEW')")
    public ResponseEntity<byte[]> export(@RequestBody AccountingExportDto request) {
        byte[] content = exportService.export(request);
        String filename = "accounting-export." + (request.getFormat() != null ? request.getFormat().toLowerCase() : "csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(content);
    }
}