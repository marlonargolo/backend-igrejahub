package com.igrejahub.modules.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.modules.dto.ModuleDto;
import com.igrejahub.modules.service.ModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Catálogo de módulos do sistema + habilitação por Igreja. Exclusivo da
 * Administração Externa (ROOT) — módulos definem o que aparece no menu do
 * app principal (Sidebar consulta via GET /auth/me → enabledModules).
 */
@RestController
@Tag(name = "Modules", description = "Catálogo de módulos e habilitação por Igreja")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class ModuleController {

    private final ModuleService moduleService;

    @Operation(summary = "Listar catálogo de módulos")
    @GetMapping("/modules")
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<List<ModuleDto>>> getModules() {
        return ResponseEntity.ok(ApiResponse.success(moduleService.getCatalog()));
    }

    @Operation(summary = "Listar módulos resolvidos para uma Igreja")
    @GetMapping("/churches/{id}/modules")
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<List<ModuleDto>>> getChurchModules(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(moduleService.getChurchModules(id)));
    }

    @Operation(summary = "Habilitar/desabilitar um módulo para uma Igreja")
    @PatchMapping("/churches/{id}/modules/{moduleId}")
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<Void>> setChurchModule(
            @PathVariable Long id, @PathVariable Long moduleId,
            @RequestBody Map<String, Boolean> body) {
        moduleService.setChurchModule(id, moduleId, Boolean.TRUE.equals(body.get("enabled")));
        return ResponseEntity.ok(ApiResponse.success());
    }
}
