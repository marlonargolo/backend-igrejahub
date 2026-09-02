package com.igrejahub.support.repository;

import com.igrejahub.support.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    // Existente — mantido
    List<SupportTicket> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    // Filtrado por Igreja (admin/pastor e ROOT com contexto)
    @Query("SELECT t FROM SupportTicket t WHERE t.organizationId = :orgId " +
           "AND (t.deleted IS NULL OR t.deleted = false) " +
           "AND (:churchId IS NULL OR t.churchId = :churchId) " +
           "ORDER BY t.createdAt DESC")
    List<SupportTicket> findByOrganizationIdAndChurchId(
        @Param("orgId") Long orgId,
        @Param("churchId") Long churchId);

    // Só tickets do próprio usuário (usuário comum)
    @Query("SELECT t FROM SupportTicket t WHERE t.organizationId = :orgId " +
           "AND (t.deleted IS NULL OR t.deleted = false) " +
           "AND t.userId = :userId " +
           "ORDER BY t.createdAt DESC")
    List<SupportTicket> findByOrganizationIdAndUserId(
        @Param("orgId") Long orgId,
        @Param("userId") Long userId);

    // Por congregação (pastor de congregação)
    @Query("SELECT t FROM SupportTicket t WHERE t.organizationId = :orgId " +
           "AND (t.deleted IS NULL OR t.deleted = false) " +
           "AND t.congregationId = :congregationId " +
           "ORDER BY t.createdAt DESC")
    List<SupportTicket> findByOrganizationIdAndCongregationId(
        @Param("orgId") Long orgId,
        @Param("congregationId") Long congregationId);
}