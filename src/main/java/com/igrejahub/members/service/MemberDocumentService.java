package com.igrejahub.members.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.members.dto.MemberDocumentDto;
import com.igrejahub.members.entity.MemberDocument;
import com.igrejahub.members.repository.MemberDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberDocumentService {

    private static final Set<String> DOCUMENT_TYPES = Set.of(
        "FICHA", "TERMO", "RG", "CPF", "COMPROVANTE_RESIDENCIA", "OUTROS");

    private static final Path UPLOAD_DIR = Paths.get("/app/uploads/member-documents");

    private final MemberDocumentRepository documentRepository;
    private final MemberService memberService;

    public List<MemberDocumentDto> getDocuments(Long memberId) {
        memberService.getMember(memberId); // reaproveita isolamento (org/Igreja/Congregação)
        return documentRepository.findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(memberId)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public MemberDocumentDto uploadDocument(Long memberId, MultipartFile file, String documentType) throws IOException {
        memberService.getMember(memberId);
        if (documentType == null || !DOCUMENT_TYPES.contains(documentType.toUpperCase())) {
            throw new BusinessException("Tipo de documento inválido.");
        }

        Files.createDirectories(UPLOAD_DIR);
        String ext = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + ext;
        file.transferTo(UPLOAD_DIR.resolve(filename));

        MemberDocument doc = MemberDocument.builder()
            .organizationId(TenantContext.getCurrentTenant())
            .memberId(memberId)
            .documentType(documentType.toUpperCase())
            .fileName(file.getOriginalFilename())
            .fileUrl("/uploads/member-documents/" + filename)
            .fileType(file.getContentType())
            .fileSize(file.getSize())
            .uploadedBy(TenantContext.getCurrentUserId())
            .build();
        doc = documentRepository.save(doc);
        log.info("Member document '{}' uploaded for memberId={} by={}",
            documentType, memberId, TenantContext.getCurrentUserId());
        return toDto(doc);
    }

    @Transactional
    public void deleteDocument(Long memberId, Long documentId) {
        memberService.getMember(memberId);
        MemberDocument doc = findOwnedDocument(memberId, documentId);
        doc.setDeleted(true);
        documentRepository.save(doc);
    }

    /** Retorna o documento validado (membro acessível + pertence a ele) para download. */
    public MemberDocument getDocumentForDownload(Long memberId, Long documentId) {
        memberService.getMember(memberId);
        return findOwnedDocument(memberId, documentId);
    }

    private MemberDocument findOwnedDocument(Long memberId, Long documentId) {
        MemberDocument doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("MemberDocument", documentId));
        if (!doc.getMemberId().equals(memberId) || Boolean.TRUE.equals(doc.getDeleted())) {
            throw new BusinessException("Documento não encontrado para este membro.");
        }
        return doc;
    }

    private MemberDocumentDto toDto(MemberDocument d) {
        return MemberDocumentDto.builder()
            .id(d.getId())
            .memberId(d.getMemberId())
            .documentType(d.getDocumentType())
            .fileName(d.getFileName())
            .fileUrl(d.getFileUrl())
            .fileType(d.getFileType())
            .fileSize(d.getFileSize())
            .uploadedBy(d.getUploadedBy())
            .createdAt(d.getCreatedAt())
            .build();
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".bin";
        return filename.substring(filename.lastIndexOf("."));
    }
}
