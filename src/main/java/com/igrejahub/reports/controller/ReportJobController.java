package com.igrejahub.reports.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.dto.PaginatedResponse;
import com.igrejahub.reports.dto.CreateReportRequest;
import com.igrejahub.reports.dto.ReportJobDto;
import com.igrejahub.reports.enums.ReportFormat;
import com.igrejahub.reports.enums.ReportType;
import com.igrejahub.reports.service.ReportJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@Tag(name = "Reports", description = "Endpoints de relatórios")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class ReportJobController {

    private final ReportJobService reportJobService;

    @Operation(summary = "Listar relatórios do usuário")
    @GetMapping
    @PreAuthorize("hasPermission(null, 'REPORT_VIEW')")
    public ResponseEntity<ApiResponse<PaginatedResponse<ReportJobDto>>> getReports(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReportJobDto> page = reportJobService.getUserReports(pageable);
        PaginatedResponse<ReportJobDto> response = PaginatedResponse.<ReportJobDto>builder()
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

    @Operation(summary = "Buscar relatório")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'REPORT_VIEW')")
    public ResponseEntity<ApiResponse<ReportJobDto>> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(reportJobService.getReport(id)));
    }

    @Operation(summary = "Solicitar geração de relatório")
    @PostMapping
    @PreAuthorize("hasPermission(null, 'REPORT_CREATE')")
    public ResponseEntity<ApiResponse<ReportJobDto>> createReport(@Valid @RequestBody CreateReportRequest request) {
        ReportJobDto report = reportJobService.createReport(
                request.getName(),
                ReportType.valueOf(request.getType()),
                ReportFormat.valueOf(request.getFormat()),
                request.getFilters());
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @Operation(summary = "Baixar relatório")
    @GetMapping("/{id}/download")
    @PreAuthorize("hasPermission(null, 'REPORT_DOWNLOAD')")
    public ResponseEntity<ApiResponse<ReportJobDto>> downloadReport(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(reportJobService.downloadReport(id)));
    }
}
