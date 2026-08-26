package com.igrejahub.support.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SupportTicketDto {
    private Long id;
    private Long organizationId;
    private Long userId;
    private String userEmail;
    private String userName;
    private String title;
    private String category;
    private String priority;
    private String status;
    private LocalDateTime criadoEm;
    private List<SupportMessageDto> mensagens;
}