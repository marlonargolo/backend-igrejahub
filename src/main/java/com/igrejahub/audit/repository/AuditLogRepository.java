package com.igrejahub.audit.repository;

import com.igrejahub.audit.entity.AuditLog;
import com.igrejahub.common.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends BaseRepository<AuditLog, Long> {
    Page<AuditLog> findByOrganizationIdAndCreatedAtBetween(Long orgId, LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<AuditLog> findByOrganizationIdAndUserId(Long orgId, Long userId, Pageable pageable);
    Page<AuditLog> findByOrganizationIdAndAction(Long orgId, String action, Pageable pageable);
    Page<AuditLog> findByOrganizationId(Long orgId, Pageable pageable);
}
