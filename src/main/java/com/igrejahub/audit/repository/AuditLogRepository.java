package com.igrejahub.audit.repository;

import com.igrejahub.audit.entity.AuditLog;
import com.igrejahub.common.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends BaseRepository<AuditLog, Long> {
    Page<AuditLog> findByOrganizationIdAndCreatedAtBetween(Long orgId, LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<AuditLog> findByOrganizationIdAndUserId(Long orgId, Long userId, Pageable pageable);
    Page<AuditLog> findByOrganizationIdAndAction(Long orgId, String action, Pageable pageable);
    Page<AuditLog> findByOrganizationId(Long orgId, Pageable pageable);

    /**
     * Filtro combinado usado pela administração externa (ROOT): período, Igreja,
     * usuário e tipo de ação, todos opcionais.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.organizationId = :orgId " +
           "AND (:churchId IS NULL OR a.churchId = :churchId) " +
           "AND (:userId IS NULL OR a.userId = :userId) " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:startDate IS NULL OR a.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findByFilters(
        @Param("orgId") Long orgId,
        @Param("churchId") Long churchId,
        @Param("userId") Long userId,
        @Param("action") String action,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);
}
