package com.igrejahub.congregations.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.dto.PaginatedResponse;
import com.igrejahub.congregations.dto.CongregationDto;
import com.igrejahub.congregations.dto.CreateCongregationRequest;
import com.igrejahub.congregations.dto.UpdateCongregationRequest;
import com.igrejahub.congregations.service.CongregationService;
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

/**
 * Endpoints de Congregações.
 *
 * Permissões usadas — todas existem no banco desde V22:
 *   GET  → SETTINGS_VIEW   (qualquer usuário com acesso ao sistema vê as congregações da sua Igreja)
 *   POST → SETTINGS_UPDATE (criar congregação — admin da Igreja)
 *   PUT  → SETTINGS_UPDATE
 *   DEL  → ROOT_ACCESS     (excluir congregação — só ROOT)
 *
 * O isolamento de Igreja é aplicado no CongregationService via TenantContext.getCurrentChurchId().
 */
@RestController
@RequestMapping("/congregations")
@Tag(name = "Congregations", description = "Endpoints de congregações")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class CongregationController {

    private final CongregationService congregationService;

    @Operation(summary = "Listar congregações da Igreja do usuário")
    @GetMapping
    @PreAuthorize("hasPermission(null, 'SETTINGS_VIEW')")
    public ResponseEntity<ApiResponse<PaginatedResponse<CongregationDto>>> getCongregations(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) Long churchId,
            @RequestParam(required = false) String search) {
        Page<CongregationDto> page = congregationService.getCongregations(pageable, churchId, search);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.<CongregationDto>builder()
            .data(page.getContent())
            .meta(PaginatedResponse.PaginationMeta.builder()
                .page(page.getNumber() + 1).pageSize(page.getSize())
                .total(page.getTotalElements()).totalPages(page.getTotalPages()).build())
            .build()));
    }

    @Operation(summary = "Buscar congregação")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'SETTINGS_VIEW')")
    public ResponseEntity<ApiResponse<CongregationDto>> getCongregation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(congregationService.getCongregation(id)));
    }

    @Operation(summary = "Criar congregação")
    @PostMapping
    @PreAuthorize("hasPermission(null, 'SETTINGS_UPDATE')")
    public ResponseEntity<ApiResponse<CongregationDto>> createCongregation(
            @Valid @RequestBody CreateCongregationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(congregationService.createCongregation(request)));
    }

    @Operation(summary = "Atualizar congregação")
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'SETTINGS_UPDATE')")
    public ResponseEntity<ApiResponse<CongregationDto>> updateCongregation(
            @PathVariable Long id, @Valid @RequestBody UpdateCongregationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(congregationService.updateCongregation(id, request)));
    }

    @Operation(summary = "Excluir congregação")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS') or hasPermission(null, 'SETTINGS_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> deleteCongregation(@PathVariable Long id) {
        congregationService.deleteCongregation(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}