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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/churches")
@Tag(name = "Churches", description = "Endpoints de igrejas")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class ChurchController {

    private final ChurchService churchService;
    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "Listar igrejas")
    @GetMapping
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
    public ResponseEntity<ApiResponse<ChurchDto>> createChurch(@Valid @RequestBody CreateChurchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(churchService.createChurch(request)));
    }

    @Operation(summary = "Atualizar igreja")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ChurchDto>> updateChurch(
            @PathVariable Long id, @Valid @RequestBody UpdateChurchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(churchService.updateChurch(id, request)));
    }

    /**
     * Upload de logo da igreja.
     * Salva o arquivo em /app/uploads/logos/ e retorna a URL pública.
     * Em produção, configure um servidor de arquivos ou CDN na frente desta pasta.
     */
    @Operation(summary = "Upload de logo")
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
        Path dest = uploadDir.resolve(fileName);
        file.transferTo(dest.toFile());

        // URL acessível externamente — ajuste o host conforme o servidor
        String logoUrl = "/api/files/logos/" + fileName;

        // Atualizar o campo logoUrl na entidade
        UpdateChurchRequest update = new UpdateChurchRequest();
        update.setLogoUrl(logoUrl);
        churchService.updateChurch(id, update);

        return ResponseEntity.ok(ApiResponse.success(Map.of("logoUrl", logoUrl)));
    }

    @Operation(summary = "Excluir igreja")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChurch(@PathVariable Long id) {
        churchService.deleteChurch(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * Retorna as igrejas que o usuário logado tem acesso.
     * ROOT/ADMIN veem todas. Demais usuários veem apenas as vinculadas em user_church_access.
     * Não requer permission específica — apenas autenticação.
     */
    @Operation(summary = "Igrejas do usuário logado")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<java.util.List<ChurchDto>>> getMyChurches() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();

        Long userId = null;
        Long orgId = null;

        try {
            userId = (Long) principal.getClass().getMethod("getUserId").invoke(principal);
            orgId  = (Long) principal.getClass().getMethod("getOrganizationId").invoke(principal);
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(java.util.Collections.emptyList()));
        }

        // Verificar se tem acesso irrestrito: ROOT ou ADMIN via role_permissions
        // Simplificado: se tem user_church_access → filtrado; se não tem → todas da org
        final Long finalUserId = userId;
        final Long finalOrgId  = orgId;

        long accessCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_church_access WHERE user_id = ?",
            Long.class, finalUserId
        );

        java.util.List<ChurchDto> result;

        if (accessCount == 0) {
            // Sem vínculos específicos → retorna TODAS as igrejas da organização (ROOT/ADMIN)
            result = churchService.getChurches(
                org.springframework.data.domain.PageRequest.of(0, 200,
                    org.springframework.data.domain.Sort.by("name")),
                null
            ).getContent();
        } else {
            // Com vínculos → retorna apenas as vinculadas
            java.util.List<java.util.Map<String,Object>> rows = jdbcTemplate.queryForList(
                "SELECT c.id FROM churches c " +
                "JOIN user_church_access uca ON c.id = uca.church_id " +
                "WHERE uca.user_id = ? AND c.organization_id = ? AND c.deleted = false",
                finalUserId, finalOrgId
            );
            result = rows.stream()
                .map(r -> {
                    try {
                        return churchService.getChurch(((Number) r.get("id")).longValue());
                    } catch (Exception ex) { return null; }
                })
                .filter(dto -> dto != null)
                .collect(java.util.stream.Collectors.toList());
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}