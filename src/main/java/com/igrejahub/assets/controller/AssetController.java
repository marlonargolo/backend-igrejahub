package com.igrejahub.assets.controller;

import com.igrejahub.assets.dto.AssetDto;
import com.igrejahub.assets.dto.CreateAssetRequest;
import com.igrejahub.assets.dto.UpdateAssetRequest;
import com.igrejahub.assets.service.AssetService;
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
@RequestMapping("/assets")
@Tag(name = "Assets", description = "Endpoints de patrimônio")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class AssetController {

    private final AssetService assetService;

    @Operation(summary = "Listar patrimônio")
    @GetMapping
    @PreAuthorize("hasPermission(null, 'ASSET_VIEW')")
    public ResponseEntity<ApiResponse<PaginatedResponse<AssetDto>>> getAssets(
            @PageableDefault(size = 20, sort = "description", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        Page<AssetDto> page = assetService.getAssets(pageable, categoryId, search, status);
        PaginatedResponse<AssetDto> response = PaginatedResponse.<AssetDto>builder()
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

    @Operation(summary = "Buscar patrimônio")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ASSET_VIEW')")
    public ResponseEntity<ApiResponse<AssetDto>> getAsset(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(assetService.getAsset(id)));
    }

    @Operation(summary = "Criar patrimônio")
    @PostMapping
    @PreAuthorize("hasPermission(null, 'ASSET_CREATE')")
    public ResponseEntity<ApiResponse<AssetDto>> createAsset(@Valid @RequestBody CreateAssetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(assetService.createAsset(request)));
    }

    @Operation(summary = "Atualizar patrimônio")
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ASSET_UPDATE')")
    public ResponseEntity<ApiResponse<AssetDto>> updateAsset(
            @PathVariable Long id, @RequestBody UpdateAssetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(assetService.updateAsset(id, request)));
    }

    @Operation(summary = "Excluir (baixar) patrimônio")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ASSET_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "Registrar baixa de patrimônio com motivo")
    @PatchMapping("/{id}/write-off")
    @PreAuthorize("hasPermission(null, 'ASSET_DELETE')")
    public ResponseEntity<ApiResponse<Void>> writeOffAsset(
            @PathVariable Long id, @RequestParam String reason) {
        assetService.writeOffAsset(id, reason);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
