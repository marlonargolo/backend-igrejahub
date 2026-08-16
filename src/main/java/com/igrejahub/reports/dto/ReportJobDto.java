package com.igrejahub.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportJobDto {
    private Long id;
    private String name;
    private String type;
    private String format;
    private String status;
    private Object filters;
    private String fileUrl;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
}
