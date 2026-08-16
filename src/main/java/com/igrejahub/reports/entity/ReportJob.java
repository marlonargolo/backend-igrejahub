package com.igrejahub.reports.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @Column(name = "type", nullable = false, length = 50)
    private String type;
    @Column(name = "format", nullable = false, length = 10)
    private String format;
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default private String status = "PENDING";
    @Column(name = "filters", columnDefinition = "jsonb")
    private Object filters;
    @Column(name = "file_url", length = 500)
    private String fileUrl;
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        expiresAt = LocalDateTime.now().plusDays(7);
    }
}
