package com.igrejahub.documents.controller;

import com.igrejahub.audit.service.AuditLogService;
import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.dto.PaginatedResponse;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.documents.entity.IgrejaDocument;
import com.igrejahub.documents.repository.DocumentRepository;
import com.igrejahub.security.SecurityUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/documents")
@Tag(name = "Documents")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final SecurityUtils       securityUtils;
    private final AuditLogService     auditLogService;

    private static final Path UPLOAD_DIR = Paths.get("/app/uploads/documents");

    @GetMapping
    @PreAuthorize("hasPermission(null, 'SETTINGS_VIEW')")
    public ResponseEntity<ApiResponse<PaginatedResponse<Map<String, Object>>>> getDocuments(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long orgId    = TenantContext.getCurrentTenant();
        Long churchId = securityUtils.getEffectiveChurchId();
        Long congId   = TenantContext.getCurrentCongregationId();

        Page<IgrejaDocument> page;
        if (securityUtils.canViewAll()) {
            page = documentRepository.findByOrganizationIdAndDeletedFalse(orgId, pageable);
        } else if (congId != null) {
            page = documentRepository.findForCongregation(orgId, churchId, congId, pageable);
        } else if (churchId != null) {
            page = documentRepository.findByOrganizationIdAndChurchId(orgId, churchId, pageable);
        } else {
            page = Page.empty(pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.<Map<String, Object>>builder()
            .data(page.map(this::toMap).getContent())
            .meta(PaginatedResponse.PaginationMeta.builder()
                .page(page.getNumber() + 1).pageSize(page.getSize())
                .total(page.getTotalElements()).totalPages(page.getTotalPages()).build())
            .build()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDocument(
            @RequestParam("file")            MultipartFile file,
            @RequestParam("title")           String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("churchId")        Long churchId,
            @RequestParam(value = "congregationId", required = false) Long congregationId) throws IOException {

        Long orgId  = TenantContext.getCurrentTenant();
        Long userId = securityUtils.getCurrentUserId();

        Files.createDirectories(UPLOAD_DIR);
        String ext      = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + ext;
        file.transferTo(UPLOAD_DIR.resolve(filename));

        IgrejaDocument doc = IgrejaDocument.builder()
            .churchId(churchId).congregationId(congregationId)
            .title(title).description(description)
            .fileName(file.getOriginalFilename())
            .fileUrl("/uploads/documents/" + filename)
            .fileType(file.getContentType()).fileSize(file.getSize())
            .uploadedBy(userId).build();
        doc.setOrganizationId(orgId);
        doc = documentRepository.save(doc);

        log.info("Document '{}' uploaded to churchId={} by={}", title, churchId, userId);
        auditLogService.logAction("UPLOAD_DOCUMENT", "DOCUMENT", doc.getId(), null,
            Map.of("title", title, "churchId", String.valueOf(churchId)));
        return ResponseEntity.ok(ApiResponse.success(toMap(doc)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'ROOT_ACCESS')")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        IgrejaDocument doc = documentRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Documento não encontrado"));
        if (!doc.getOrganizationId().equals(orgId)) throw new BusinessException("Acesso não autorizado");
        doc.setDeleted(true);
        doc.setUpdatedAt(LocalDateTime.now());
        documentRepository.save(doc);
        auditLogService.logAction("DELETE_DOCUMENT", "DOCUMENT", doc.getId(), null, null);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasPermission(null, 'SETTINGS_VIEW')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        Long orgId    = TenantContext.getCurrentTenant();
        Long churchId = securityUtils.getEffectiveChurchId();

        IgrejaDocument doc = documentRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Documento não encontrado"));
        if (!doc.getOrganizationId().equals(orgId)) throw new BusinessException("Acesso não autorizado");
        if (!securityUtils.canViewAll() && churchId != null && !churchId.equals(doc.getChurchId())) {
            throw new BusinessException("Você não tem acesso a este documento");
        }

        Path file = Paths.get("/app" + doc.getFileUrl());
        if (!Files.exists(file)) throw new BusinessException("Arquivo não encontrado no servidor");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
            .contentType(MediaType.parseMediaType(
                doc.getFileType() != null ? doc.getFileType() : "application/octet-stream"))
            .body(new FileSystemResource(file));
    }

    private Map<String, Object> toMap(IgrejaDocument d) {
        return Map.of(
            "id",             d.getId(),
            "title",          d.getTitle(),
            "description",    d.getDescription() != null ? d.getDescription() : "",
            "fileName",       d.getFileName(),
            "fileUrl",        d.getFileUrl(),
            "fileType",       d.getFileType() != null ? d.getFileType() : "",
            "fileSize",       d.getFileSize() != null ? d.getFileSize() : 0L,
            "churchId",       d.getChurchId(),
            "congregationId", d.getCongregationId() != null ? d.getCongregationId() : "",
            "createdAt",      d.getCreatedAt() != null ? d.getCreatedAt().toString() : ""
        );
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".bin";
        return filename.substring(filename.lastIndexOf("."));
    }
}