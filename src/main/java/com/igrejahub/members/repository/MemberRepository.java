package com.igrejahub.members.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.members.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends BaseRepository<Member, Long> {

    // ── Existentes — não remover ──────────────────────────────────────────────
    Page<Member> findByOrganizationId(Long organizationId, Pageable pageable);
    Page<Member> findByOrganizationIdAndNameContainingIgnoreCase(Long organizationId, String name, Pageable pageable);
    long countByOrganizationIdAndStatus(Long organizationId, String status);

    /**
     * Busca original — mantida para compatibilidade.
     * Para isolamento, use findByChurchId ou findByCongregationId.
     */
    @Query("SELECT m FROM Member m WHERE m.organizationId = :orgId " +
           "AND (:churchId IS NULL OR m.churchId = :churchId) " +
           "AND (:congregationId IS NULL OR m.congregationId = :congregationId) " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:search IS NULL OR LOWER(m.name) LIKE LOWER(CAST(:search AS string)))")
    Page<Member> search(@Param("orgId") Long orgId,
                        @Param("churchId") Long churchId,
                        @Param("congregationId") Long congregationId,
                        @Param("status") String status,
                        @Param("search") String search,
                        Pageable pageable);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.organizationId = :orgId AND m.status = :status " +
           "AND (:churchId IS NULL OR m.churchId = :churchId) " +
           "AND (:congregationId IS NULL OR m.congregationId = :congregationId)")
    long countByFilter(@Param("orgId") Long orgId,
                       @Param("status") String status,
                       @Param("churchId") Long churchId,
                       @Param("congregationId") Long congregationId);

    // ── Busca com churchId OBRIGATÓRIO (admin/pastor da igreja) ───────────────
    @Query("SELECT m FROM Member m WHERE m.organizationId = :orgId " +
           "AND m.churchId = :churchId " +
           "AND (:congregationId IS NULL OR m.congregationId = :congregationId) " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:search IS NULL OR LOWER(m.name) LIKE LOWER(CAST(:search AS string)))")
    Page<Member> findByChurchId(@Param("orgId") Long orgId,
                                @Param("churchId") Long churchId,
                                @Param("congregationId") Long congregationId,
                                @Param("status") String status,
                                @Param("search") String search,
                                Pageable pageable);

    // ── Busca por congregação (pastor de congregação) ─────────────────────────
    @Query("SELECT m FROM Member m WHERE m.organizationId = :orgId " +
           "AND m.congregationId = :congregationId " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:search IS NULL OR LOWER(m.name) LIKE LOWER(CAST(:search AS string)))")
    Page<Member> findByCongregationId(@Param("orgId") Long orgId,
                                      @Param("congregationId") Long congregationId,
                                      @Param("status") String status,
                                      @Param("search") String search,
                                      Pageable pageable);

    // ── ROOT modo global: todos da organização ────────────────────────────────
    @Query("SELECT m FROM Member m WHERE m.organizationId = :orgId " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:search IS NULL OR LOWER(m.name) LIKE LOWER(CAST(:search AS string)))")
    Page<Member> findAllByOrganization(@Param("orgId") Long orgId,
                                       @Param("status") String status,
                                       @Param("search") String search,
                                       Pageable pageable);

    // ── Contagem por igreja (quota do plano) ──────────────────────────────────
    @Query("SELECT COUNT(m) FROM Member m WHERE m.churchId = :churchId AND m.deleted = false")
    long countByChurchId(@Param("churchId") Long churchId);
}