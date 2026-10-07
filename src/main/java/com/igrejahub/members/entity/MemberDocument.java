package com.igrejahub.members.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MemberDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** FICHA, TERMO, RG, CPF, COMPROVANTE_RESIDENCIA ou OUTROS. */
    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType;

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

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "deleted")
    @Builder.Default
    private Boolean deleted = false;
}
