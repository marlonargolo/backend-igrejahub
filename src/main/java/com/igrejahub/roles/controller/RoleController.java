package com.igrejahub.roles.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.dto.PaginatedResponse;
import com.igrejahub.roles.dto.CreateRoleRequest;
import com.igrejahub.roles.dto.RoleDto;
import com.igrejahub.roles.dto.UpdateRoleRequest;
import com.igrejahub.roles.service.RoleService;
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
@RequestMapping("/roles")
@Tag(name = "Roles", description = "Endpoints de roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
public class RoleController {
    private final RoleService roleService;

    @Operation(summary = "Listar roles")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<RoleDto>>> getRoles(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<RoleDto> page = roleService.getRoles(pageable);
        PaginatedResponse<RoleDto> response = PaginatedResponse.<RoleDto>builder()
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

    @Operation(summary = "Buscar role")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDto>> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getRole(id)));
    }

    @Operation(summary = "Criar role")
    @PostMapping
    public ResponseEntity<ApiResponse<RoleDto>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(roleService.createRole(request)));
    }

    @Operation(summary = "Atualizar role")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDto>> updateRole(
            @PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(roleService.updateRole(id, request)));
    }

    @Operation(summary = "Excluir role")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
