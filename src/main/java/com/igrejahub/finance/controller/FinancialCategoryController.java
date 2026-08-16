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
@Tag(name = "Finance Categories", description = "Categorias financeiras")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class FinancialCategoryController {

    private final FinancialCategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
    public ResponseEntity<ApiResponse<Page<FinancialCategoryDto>>> getCategories(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategories(pageable)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
    public ResponseEntity<ApiResponse<List<FinancialCategoryDto>>> getActiveCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getActiveCategories()));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'FINANCE_MANAGE')")
    public ResponseEntity<ApiResponse<FinancialCategoryDto>> createCategory(@RequestBody Map<String, String> body) {
        var dto = categoryService.createCategory(body.get("name"), body.get("type"), body.get("color"));
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'FINANCE_MANAGE')")
    public ResponseEntity<ApiResponse<FinancialCategoryDto>> updateCategory(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String color = (String) body.get("color");
        Boolean active = body.get("active") != null ? (Boolean) body.get("active") : null;
        return ResponseEntity.ok(ApiResponse.success(categoryService.updateCategory(id, name, color, active)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'FINANCE_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}