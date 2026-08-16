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

@RestController
@RequestMapping("/congregations")
@Tag(name = "Congregations", description = "Endpoints de congregações")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class CongregationController {

    private final CongregationService congregationService;

    @Operation(summary = "Listar congregações")
    @GetMapping
    @PreAuthorize("hasPermission(null, 'CHURCH_VIEW')")
    public ResponseEntity<ApiResponse<PaginatedResponse<CongregationDto>>> getCongregations(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) Long churchId,
            @RequestParam(required = false) String search) {
        Page<CongregationDto> page = congregationService.getCongregations(pageable, churchId, search);
        PaginatedResponse<CongregationDto> response = PaginatedResponse.<CongregationDto>builder()
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

    @Operation(summary = "Buscar congregação")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CHURCH_VIEW')")
    public ResponseEntity<ApiResponse<CongregationDto>> getCongregation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(congregationService.getCongregation(id)));
    }

    @Operation(summary = "Criar congregação")
    @PostMapping
    @PreAuthorize("hasPermission(null, 'CHURCH_CREATE')")
    public ResponseEntity<ApiResponse<CongregationDto>> createCongregation(
            @Valid @RequestBody CreateCongregationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(congregationService.createCongregation(request)));
    }

    @Operation(summary = "Atualizar congregação")
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CHURCH_UPDATE')")
    public ResponseEntity<ApiResponse<CongregationDto>> updateCongregation(
            @PathVariable Long id, @Valid @RequestBody UpdateCongregationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(congregationService.updateCongregation(id, request)));
    }

    @Operation(summary = "Excluir congregação")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CHURCH_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteCongregation(@PathVariable Long id) {
        congregationService.deleteCongregation(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
