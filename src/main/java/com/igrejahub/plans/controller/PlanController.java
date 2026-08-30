package com.igrejahub.plans.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.plans.dto.CreatePlanRequest;
import com.igrejahub.plans.dto.PlanDto;
import com.igrejahub.plans.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/plans")
@Tag(name = "Plans", description = "Gerenciamento de planos (apenas ROOT)")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
public class PlanController {

    private final PlanService planService;

    @Operation(summary = "Listar todos os planos ativos")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(planService.listPlans()));
    }

    @Operation(summary = "Buscar plano por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(planService.getPlan(id)));
    }

    @Operation(summary = "Criar novo plano")
    @PostMapping
    public ResponseEntity<ApiResponse<PlanDto>> create(@Valid @RequestBody CreatePlanRequest req) {
        return ResponseEntity.ok(ApiResponse.success(planService.createPlan(req)));
    }

    @Operation(summary = "Atualizar plano")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanDto>> update(
            @PathVariable Long id, @Valid @RequestBody CreatePlanRequest req) {
        return ResponseEntity.ok(ApiResponse.success(planService.updatePlan(id, req)));
    }

    @Operation(summary = "Desativar plano")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        planService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}