package com.igrejahub.churches.controller;

import com.igrejahub.churches.dto.*;
import com.igrejahub.churches.service.ChurchService;
import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.dto.PaginatedResponse;
import com.igrejahub.security.SecurityUtils;
import com.igrejahub.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/churches")
@Tag(name = "Churches", description = "Endpoints de igrejas")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class ChurchController {

    private final ChurchService churchService;
    private final SecurityUtils securityUtils;

    // ─── Listagem ─────────────────────────────────────────────────────────────

    /**
     * ROOT → todas as igrejas.
     * Outros → apenas a própria Igreja (filtrado no service).
     */
    @Operation(summary = "Listar igrejas (escopo por perfil)")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<ChurchDto>>> getChurches(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String search) {

        Page<ChurchDto> page = churchService.getChurches(pageable, search);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.<ChurchDto>builder()
                .data(page.getContent())
                .meta(PaginatedResponse.PaginationMeta.builder()
                        .page(page.getNumber() + 1)
                        .pageSize(page.getSize())
                        .total(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build()));
    }

    /**
     * Igrejas acessíveis pelo usuário logado (usado pela tela de seleção de Igreja).
     *
     * Lógica de escopo aplicada no ChurchService.getChurches():
     *   ROOT            → todas as igrejas
     *   Admin de Igreja → só a própria Igreja
     *   Pastor/Tesoureiro/Congregação → só a própria Igreja (e suas congregações no service de congregações)
     */
    @Operation(summary = "Igrejas do usuário logado")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ChurchDto>>> getMyChurches(
            @PageableDefault(size = 200, sort = "name") Pageable pageable) {
        Page<ChurchDto> page = churchService.getChurches(pageable, null);
        return ResponseEntity.ok(ApiResponse.success(page.getContent()));
    }

    @Operation(summary = "Buscar igreja por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChurchDto>> getChurch(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(churchService.getChurch(id)));
    }

    // ─── CRUD (ROOT) ──────────────────────────────────────────────────────────

    /**
     * Criar Igreja — EXCLUSIVO ROOT.
     * Vincula Plano e cria usuário admin da Igreja automaticamente.
     */
    @Operation(summary = "Criar Igreja (apenas ROOT)")
    @PostMapping
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<ChurchDto>> createChurch(
            @Valid @RequestBody CreateChurchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(churchService.createChurch(request)));
    }

    @Operation(summary = "Atualizar Igreja")
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'SETTINGS_UPDATE') or hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<ChurchDto>> updateChurch(
            @PathVariable Long id, @Valid @RequestBody UpdateChurchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(churchService.updateChurch(id, request)));
    }

    /**
     * Excluir (desativar) Igreja — EXCLUSIVO ROOT.
     */
    @Operation(summary = "Excluir Igreja (apenas ROOT)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<Void>> deleteChurch(@PathVariable Long id) {
        churchService.deleteChurch(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ─── Upload de logo ───────────────────────────────────────────────────────

    @Operation(summary = "Upload de logo")
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission(null, 'SETTINGS_UPDATE') or hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {

        String ext = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        String fileName = "church_" + id + "_" + UUID.randomUUID() + ext;
        Path uploadDir = Paths.get("/app/uploads/logos");
        Files.createDirectories(uploadDir);
        file.transferTo(uploadDir.resolve(fileName).toFile());

        String logoUrl = "/api/files/logos/" + fileName;
        UpdateChurchRequest update = new UpdateChurchRequest();
        update.setLogoUrl(logoUrl);
        churchService.updateChurch(id, update);

        return ResponseEntity.ok(ApiResponse.success(Map.of("logoUrl", logoUrl)));
    }
}
