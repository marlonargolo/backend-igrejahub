package com.igrejahub.permissions.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.permissions.dto.PermissionDto;
import com.igrejahub.permissions.service.PermissionService;
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
@RequestMapping("/permissions")
@Tag(name = "Permissions", description = "Endpoints de permissões")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
public class PermissionController {
    private final PermissionService permissionService;

    @Operation(summary = "Listar todas as permissões")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getAllPermissions() {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getAllPermissions()));
    }

    @Operation(summary = "Listar permissões ativas")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getActivePermissions() {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getActivePermissions()));
    }

    @Operation(summary = "Buscar permissão")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionDto>> getPermission(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getPermission(id)));
    }

    @Operation(summary = "Criar permissão")
    @PostMapping
    public ResponseEntity<ApiResponse<PermissionDto>> createPermission(@Valid @RequestBody PermissionDto dto) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.createPermission(dto)));
    }

    @Operation(summary = "Atualizar permissão")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionDto>> updatePermission(
            @PathVariable Long id, @Valid @RequestBody PermissionDto dto) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.updatePermission(id, dto)));
    }
}
