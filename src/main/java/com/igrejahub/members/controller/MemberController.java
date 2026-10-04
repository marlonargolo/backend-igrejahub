package com.igrejahub.members.controller;

import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.dto.PaginatedResponse;
import com.igrejahub.members.dto.CreateMemberRequest;
import com.igrejahub.members.dto.MemberDocumentDto;
import com.igrejahub.members.dto.MemberDto;
import com.igrejahub.members.dto.MemberTransferDto;
import com.igrejahub.members.dto.TransferMemberRequest;
import com.igrejahub.members.dto.UpdateMemberRequest;
import com.igrejahub.members.entity.MemberDocument;
import com.igrejahub.members.service.MemberDocumentService;
import com.igrejahub.members.service.MemberService;
import com.igrejahub.members.service.MemberTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.igrejahub.members.dto.CreateOccurrenceRequest;
import com.igrejahub.members.dto.MemberOccurrenceDto;
import com.igrejahub.members.service.MemberOccurrenceService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/members")
@Tag(name = "Members", description = "Endpoints de membros")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "Listar membros")
    @GetMapping
    @PreAuthorize("hasPermission(null, 'MEMBER_VIEW')")
    public ResponseEntity<ApiResponse<PaginatedResponse<MemberDto>>> getMembers(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) Long churchId,
            @RequestParam(required = false) Long congregationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        Page<MemberDto> page = memberService.getMembers(pageable, churchId, congregationId, status, search);

        PaginatedResponse<MemberDto> response = PaginatedResponse.<MemberDto>builder()
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

    @Operation(summary = "Buscar membro")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'MEMBER_VIEW')")
    public ResponseEntity<ApiResponse<MemberDto>> getMember(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getMember(id)));
    }

    @Operation(summary = "Criar membro")
    @PostMapping
    @PreAuthorize("hasPermission(null, 'MEMBER_CREATE')")
    public ResponseEntity<ApiResponse<MemberDto>> createMember(@Valid @RequestBody CreateMemberRequest request) {
        return ResponseEntity.ok(ApiResponse.success(memberService.createMember(request)));
    }

    @Operation(summary = "Atualizar membro")
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'MEMBER_UPDATE')")
    public ResponseEntity<ApiResponse<MemberDto>> updateMember(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberRequest request) {
        return ResponseEntity.ok(ApiResponse.success(memberService.updateMember(id, request)));
    }

    @Operation(summary = "Excluir membro")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'MEMBER_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "Histórico de alterações do membro")
    @GetMapping("/{id}/history")
    @PreAuthorize("hasPermission(null, 'MEMBER_VIEW')")
    public ResponseEntity<ApiResponse<PaginatedResponse<com.igrejahub.audit.entity.AuditLog>>> getMemberHistory(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<com.igrejahub.audit.entity.AuditLog> page = memberService.getMemberHistory(id, pageable);
        PaginatedResponse<com.igrejahub.audit.entity.AuditLog> response =
            PaginatedResponse.<com.igrejahub.audit.entity.AuditLog>builder()
                .data(page.getContent())
                .meta(PaginatedResponse.PaginationMeta.builder()
                    .page(page.getNumber() + 1).pageSize(page.getSize())
                    .total(page.getTotalElements()).totalPages(page.getTotalPages()).build())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private final MemberOccurrenceService occurrenceService;
    private final MemberDocumentService memberDocumentService;
    private final MemberTransferService memberTransferService;

    @Operation(summary = "Transferir membro para outra Congregação")
    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasPermission(null, 'MEMBER_UPDATE')")
    public ResponseEntity<ApiResponse<MemberTransferDto>> transferMember(
            @PathVariable Long id, @Valid @RequestBody TransferMemberRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            memberTransferService.transferMember(id, request.getToCongregationId(), request.getReason())));
    }

    @Operation(summary = "Listar transferências registradas")
    @GetMapping("/transfers")
    @PreAuthorize("hasPermission(null, 'MEMBER_VIEW')")
    public ResponseEntity<ApiResponse<PaginatedResponse<MemberTransferDto>>> getTransfers(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MemberTransferDto> page = memberTransferService.listTransfers(pageable);
        PaginatedResponse<MemberTransferDto> response = PaginatedResponse.<MemberTransferDto>builder()
            .data(page.getContent())
            .meta(PaginatedResponse.PaginationMeta.builder()
                .page(page.getNumber() + 1).pageSize(page.getSize())
                .total(page.getTotalElements()).totalPages(page.getTotalPages()).build())
            .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasPermission(null, 'MEMBER_VIEW')")
    public ResponseEntity<ApiResponse<List<MemberDocumentDto>>> getMemberDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(memberDocumentService.getDocuments(id)));
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission(null, 'MEMBER_UPDATE')")
    public ResponseEntity<ApiResponse<MemberDocumentDto>> uploadMemberDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType) throws IOException {
        return ResponseEntity.ok(ApiResponse.success(memberDocumentService.uploadDocument(id, file, documentType)));
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @PreAuthorize("hasPermission(null, 'MEMBER_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> deleteMemberDocument(
            @PathVariable Long id, @PathVariable Long documentId) {
        memberDocumentService.deleteDocument(id, documentId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{id}/documents/{documentId}/download")
    @PreAuthorize("hasPermission(null, 'MEMBER_VIEW')")
    public ResponseEntity<Resource> downloadMemberDocument(
            @PathVariable Long id, @PathVariable Long documentId) {
        MemberDocument doc = memberDocumentService.getDocumentForDownload(id, documentId);
        Path file = Paths.get("/app" + doc.getFileUrl());
        if (!Files.exists(file)) throw new com.igrejahub.common.exception.BusinessException("Arquivo não encontrado no servidor");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
            .contentType(MediaType.parseMediaType(
                doc.getFileType() != null ? doc.getFileType() : "application/octet-stream"))
            .body(new FileSystemResource(file));
    }

    @GetMapping("/{id}/occurrences")
    public ResponseEntity<ApiResponse<List<MemberOccurrenceDto>>> getOccurrences(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(occurrenceService.getOccurrences(id)));
    }

    @PostMapping("/{id}/occurrences")
    public ResponseEntity<ApiResponse<MemberOccurrenceDto>> createOccurrence(
            @PathVariable Long id,
            @Valid @RequestBody CreateOccurrenceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(occurrenceService.createOccurrence(id, request)));
    }

    @DeleteMapping("/{id}/occurrences/{occurrenceId}")
    public ResponseEntity<ApiResponse<Void>> deleteOccurrence(
            @PathVariable Long id,
            @PathVariable Long occurrenceId) {
        occurrenceService.deleteOccurrence(occurrenceId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "Upload de avatar do membro")
    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        String ext = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = "member_" + id + "_" + UUID.randomUUID() + ext;
        Path uploadDir = Paths.get("/app/uploads/avatars");
        Files.createDirectories(uploadDir);
        file.transferTo(uploadDir.resolve(fileName).toFile());
        String avatarUrl = "/api/files/avatars/" + fileName;
        memberService.updateAvatar(id, avatarUrl);
        return ResponseEntity.ok(ApiResponse.success(Map.of("avatarUrl", avatarUrl)));
    }
}