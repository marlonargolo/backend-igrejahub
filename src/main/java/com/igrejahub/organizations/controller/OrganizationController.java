package com.igrejahub.organizations.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.dto.PaginatedResponse;
import com.igrejahub.organizations.dto.CreateOrganizationRequest;
import com.igrejahub.organizations.dto.OrganizationDto;
import com.igrejahub.organizations.dto.UpdateOrganizationRequest;
import com.igrejahub.organizations.service.OrganizationService;
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
@RequestMapping("/organizations")
@Tag(name = "Organizations", description = "Endpoints de organizações")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class OrganizationController {
    private final OrganizationService organizationService;

    @Operation(summary = "Listar organizações")
    @GetMapping
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<PaginatedResponse<OrganizationDto>>> getOrganizations(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<OrganizationDto> page = organizationService.getOrganizations(pageable);
        PaginatedResponse<OrganizationDto> response = PaginatedResponse.<OrganizationDto>builder()
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

    @Operation(summary = "Buscar organização")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<OrganizationDto>> getOrganization(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getOrganization(id)));
    }

    @Operation(summary = "Criar organização")
    @PostMapping
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<OrganizationDto>> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.createOrganization(request)));
    }

    @Operation(summary = "Atualizar organização")
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<OrganizationDto>> updateOrganization(
            @PathVariable Long id, @Valid @RequestBody UpdateOrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.updateOrganization(id, request)));
    }

    @Operation(summary = "Excluir organização")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(@PathVariable Long id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "Obter organização atual")
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrganizationDto>> getCurrentOrganization() {
        OrganizationDto org = organizationService.getOrganization(
            organizationService.getCurrentOrganization().getId());
        return ResponseEntity.ok(ApiResponse.success(org));
    }
}
