package com.igrejahub.documents.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.documents.entity.IgrejaDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends BaseRepository<IgrejaDocument, Long> {

    @Query("SELECT d FROM IgrejaDocument d WHERE d.organizationId = :orgId AND d.churchId = :churchId " +
           "AND d.deleted = false ORDER BY d.createdAt DESC")
    Page<IgrejaDocument> findByOrganizationIdAndChurchId(
        @Param("orgId") Long orgId, @Param("churchId") Long churchId, Pageable pageable);

    @Query("SELECT d FROM IgrejaDocument d WHERE d.organizationId = :orgId AND d.churchId = :churchId " +
           "AND (d.congregationId IS NULL OR d.congregationId = :congregationId) " +
           "AND d.deleted = false ORDER BY d.createdAt DESC")
    Page<IgrejaDocument> findForCongregation(
        @Param("orgId") Long orgId, @Param("churchId") Long churchId,
        @Param("congregationId") Long congregationId, Pageable pageable);

    Page<IgrejaDocument> findByOrganizationIdAndDeletedFalse(Long organizationId, Pageable pageable);
}