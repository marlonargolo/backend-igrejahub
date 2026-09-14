package com.igrejahub.documents.entity;

import com.igrejahub.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidade de documentos enviados pelo ROOT para igrejas/congregações.
 * Nome da classe: IgrejaDocument (evita conflito com java.lang.Document).
 * Nome da tabela: documents.
 */
@Entity
@Table(name = "documents")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IgrejaDocument extends BaseEntity {

    @Column(name = "church_id", nullable = false)
    private Long churchId;

    @Column(name = "congregation_id")
    private Long congregationId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}