package com.igrejahub.assets.repository;

import com.igrejahub.assets.entity.Asset;
import com.igrejahub.common.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends BaseRepository<Asset, Long> {

    // ── Existentes — não remover ──────────────────────────────────────────────
    Page<Asset> findByOrganizationId(Long organizationId, Pageable pageable);
    Optional<Asset> findByOrganizationIdAndId(Long organizationId, Long id);
    Page<Asset> findByOrganizationIdAndStatus(Long organizationId, String status, Pageable pageable);
    Page<Asset> findByOrganizationIdAndDescriptionContainingIgnoreCase(Long organizationId, String description, Pageable pageable);
    Page<Asset> findByOrganizationIdAndCategoryId(Long organizationId, Long categoryId, Pageable pageable);
    long countByOrganizationIdAndStatus(Long organizationId, String status);
    boolean existsByOrganizationIdAndCode(Long organizationId, String code);

    // ── Com churchId obrigatório (admin / pastor da Igreja / ROOT com contexto) ─
    @Query("SELECT a FROM Asset a WHERE a.organizationId = :orgId AND a.deleted = false " +
           "AND a.churchId = :churchId " +
           "AND (:categoryId IS NULL OR a.categoryId = :categoryId) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:search IS NULL OR LOWER(a.description) LIKE LOWER(CAST(:search AS string)))")
    Page<Asset> findByChurchId(@Param("orgId") Long orgId,
                               @Param("churchId") Long churchId,
                               @Param("categoryId") Long categoryId,
                               @Param("status") String status,
                               @Param("search") String search,
                               Pageable pageable);

    // ── Por congregação (PASTOR_CONGREGACAO) ──────────────────────────────────
    @Query("SELECT a FROM Asset a WHERE a.organizationId = :orgId AND a.deleted = false " +
           "AND a.congregationId = :congregationId " +
           "AND (:categoryId IS NULL OR a.categoryId = :categoryId) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:search IS NULL OR LOWER(a.description) LIKE LOWER(CAST(:search AS string)))")
    Page<Asset> findByCongregationId(@Param("orgId") Long orgId,
                                     @Param("congregationId") Long congregationId,
                                     @Param("categoryId") Long categoryId,
                                     @Param("status") String status,
                                     @Param("search") String search,
                                     Pageable pageable);

    // ── ROOT modo global: todos da organização ────────────────────────────────
    @Query("SELECT a FROM Asset a WHERE a.organizationId = :orgId AND a.deleted = false " +
           "AND (:categoryId IS NULL OR a.categoryId = :categoryId) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:search IS NULL OR LOWER(a.description) LIKE LOWER(CAST(:search AS string)))")
    Page<Asset> findAllByOrganization(@Param("orgId") Long orgId,
                                      @Param("categoryId") Long categoryId,
                                      @Param("status") String status,
                                      @Param("search") String search,
                                      Pageable pageable);

    // ── Contagem por Igreja (quota) ───────────────────────────────────────────
    @Query("SELECT COUNT(a) FROM Asset a WHERE a.churchId = :churchId AND a.deleted = false")
    long countByChurchId(@Param("churchId") Long churchId);
}