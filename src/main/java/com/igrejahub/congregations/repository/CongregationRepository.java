package com.igrejahub.congregations.repository;

import com.igrejahub.congregations.entity.Congregation;
import com.igrejahub.common.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CongregationRepository extends BaseRepository<Congregation, Long> {

    // ─── Existentes — não remover ─────────────────────────────────────────────
    Page<Congregation> findByOrganizationId(Long orgId, Pageable pageable);
    Optional<Congregation> findByOrganizationIdAndId(Long orgId, Long id);
    Page<Congregation> findByOrganizationIdAndChurchId(Long orgId, Long churchId, Pageable pageable);
    Page<Congregation> findByOrganizationIdAndNameContainingIgnoreCase(Long orgId, String name, Pageable pageable);
    long countByOrganizationIdAndStatus(Long orgId, String status);

    // ─── Novos: escopo por Igreja ─────────────────────────────────────────────
    @Query("SELECT c FROM Congregation c WHERE c.organizationId = :orgId AND c.churchId = :churchId " +
           "AND c.deleted = false AND LOWER(c.name) LIKE LOWER(CONCAT('%',:name,'%'))")
    Page<Congregation> findByOrganizationIdAndChurchIdAndNameContainingIgnoreCase(
        @Param("orgId") Long orgId,
        @Param("churchId") Long churchId,
        @Param("name") String name,
        Pageable pageable);

    long countByChurchId(Long churchId);
}