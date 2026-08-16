package com.igrejahub.audit.controller;

import com.igrejahub.audit.entity.AuditLog;
import com.igrejahub.audit.service.AuditLogService;
import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.dto.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit")
@Tag(name = "Audit", description = "Endpoints de auditoria")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasPermission(null, 'AUDIT_VIEW')")
public class AuditLogController {
    private final AuditLogService auditLogService;

    @Operation(summary = "Listar logs de auditoria")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<AuditLog>>> getAuditLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId) {
        Page<AuditLog> page = auditLogService.getAuditLogs(pageable, action, userId);
        PaginatedResponse<AuditLog> response = PaginatedResponse.<AuditLog>builder()
            .data(page.getContent())
            .meta(PaginatedResponse.PaginationMeta.builder()
                .page(page.getNumber() + 1)
                .pageSize(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build())
            .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
