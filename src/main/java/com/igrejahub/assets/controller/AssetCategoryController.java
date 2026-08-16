package com.igrejahub.assets.controller;

import com.igrejahub.assets.dto.AssetCategoryDto;
import com.igrejahub.assets.dto.CreateAssetCategoryRequest;
import com.igrejahub.assets.service.AssetCategoryService;
import com.igrejahub.common.dto.ApiResponse;
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
@RequestMapping("/asset-categories")
@Tag(name = "Asset Categories", description = "Categorias de patrimônio")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class AssetCategoryController {

    private final AssetCategoryService categoryService;

    @Operation(summary = "Listar categorias de patrimônio")
    @GetMapping
    @PreAuthorize("hasPermission(null, 'ASSET_VIEW')")
    public ResponseEntity<ApiResponse<List<AssetCategoryDto>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategories()));
    }

    @Operation(summary = "Buscar categoria de patrimônio")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ASSET_VIEW')")
    public ResponseEntity<ApiResponse<AssetCategoryDto>> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategory(id)));
    }

    @Operation(summary = "Criar categoria de patrimônio")
    @PostMapping
    @PreAuthorize("hasPermission(null, 'ASSET_CREATE')")
    public ResponseEntity<ApiResponse<AssetCategoryDto>> createCategory(
            @Valid @RequestBody CreateAssetCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.createCategory(request)));
    }
}
