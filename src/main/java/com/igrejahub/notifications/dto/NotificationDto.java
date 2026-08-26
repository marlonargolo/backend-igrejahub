package com.igrejahub.notifications.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String title;
    private String body;
    private String type;
    private Boolean read;
    private String link;
    private LocalDateTime createdAt;
}