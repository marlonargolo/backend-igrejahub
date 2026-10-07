package com.igrejahub.members.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDocumentDto {
    private Long id;
    private Long memberId;
    private String documentType;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private Long uploadedBy;
    private LocalDateTime createdAt;
}
