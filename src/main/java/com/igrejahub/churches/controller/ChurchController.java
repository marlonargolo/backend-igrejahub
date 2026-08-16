package com.igrejahub.churches.controller;

import com.igrejahub.churches.dto.ChurchDto;
import com.igrejahub.churches.dto.CreateChurchRequest;
import com.igrejahub.churches.dto.UpdateChurchRequest;
import com.igrejahub.churches.service.ChurchService;
import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.dto.PaginatedResponse;
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
@RequestMapping("/churches")
@Tag(name = "Churches", description = "Endpoints de igrejas")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class ChurchController {

    private final ChurchService churchService;

    @Operation(summary = "Listar igrejas")
    @GetMapping
    // Qualquer usuário autenticado pode listar suas igrejas (necessário para seleção de igreja no login)
    public ResponseEntity<ApiResponse<PaginatedResponse<ChurchDto>>> getChurches(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String search) {
        Page<ChurchDto> page = churchService.getChurches(pageable, search);
        PaginatedResponse<ChurchDto> response = PaginatedResponse.<ChurchDto>builder()
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

    @Operation(summary = "Buscar igreja")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChurchDto>> getChurch(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(churchService.getChurch(id)));
    }

    @Operation(summary = "Criar igreja")
    @PostMapping
    @PreAuthorize("hasPermission(null, 'CHURCH_CREATE')")
    public ResponseEntity<ApiResponse<ChurchDto>> createChurch(@Valid @RequestBody CreateChurchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(churchService.createChurch(request)));
    }

    @Operation(summary = "Atualizar igreja")
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CHURCH_UPDATE')")
    public ResponseEntity<ApiResponse<ChurchDto>> updateChurch(
            @PathVariable Long id, @Valid @RequestBody UpdateChurchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(churchService.updateChurch(id, request)));
    }

    @Operation(summary = "Excluir igreja")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CHURCH_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteChurch(@PathVariable Long id) {
        churchService.deleteChurch(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}