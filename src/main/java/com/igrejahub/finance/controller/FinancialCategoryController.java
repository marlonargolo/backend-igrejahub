package com.igrejahub.finance.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.finance.dto.FinancialCategoryDto;
import com.igrejahub.finance.service.FinancialCategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/finance/categories")
@Tag(name = "Finance Categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class FinancialCategoryController {

    private final FinancialCategoryService categoryService;

    /** Paginado — sem filtro de tipo */
    @GetMapping
    @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
    public ResponseEntity<ApiResponse<Page<FinancialCategoryDto>>> getCategories(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategories(pageable)));
    }

    /** Lista ativa — aceita ?type=REVENUE|EXPENSE para filtrar por tipo */
    @GetMapping("/active")
    @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
    public ResponseEntity<ApiResponse<List<FinancialCategoryDto>>> getActiveCategories(
            @RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(
                categoryService.getActiveCategoriesByType(type)));
        }
        return ResponseEntity.ok(ApiResponse.success(categoryService.getActiveCategories()));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
    public ResponseEntity<ApiResponse<FinancialCategoryDto>> createCategory(
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
            categoryService.createCategory(body.get("name"), body.get("type"), body.get("color"))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
    public ResponseEntity<ApiResponse<FinancialCategoryDto>> updateCategory(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.updateCategory(
            id, (String) body.get("name"), (String) body.get("color"),
            body.get("active") != null ? (Boolean) body.get("active") : null)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}