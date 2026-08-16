package com.igrejahub.dashboard.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.dashboard.dto.DashboardFilterDto;
import com.igrejahub.dashboard.dto.DashboardMetrics;
import com.igrejahub.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Endpoints do dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class DashboardController {
    private final DashboardService dashboardService;

    @Operation(summary = "Obter métricas do dashboard")
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardMetrics>> getDashboardMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long churchId,
            @RequestParam(required = false) Long congregationId) {
        DashboardFilterDto filter = DashboardFilterDto.builder()
            .startDate(startDate).endDate(endDate).churchId(churchId).congregationId(congregationId).build();
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboardMetrics(filter)));
    }
}
