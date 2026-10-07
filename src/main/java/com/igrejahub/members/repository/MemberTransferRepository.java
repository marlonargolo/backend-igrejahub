package com.igrejahub.members.repository;

import com.igrejahub.members.entity.MemberTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberTransferRepository extends JpaRepository<MemberTransfer, Long> {

    Page<MemberTransfer> findByOrganizationIdOrderByTransferredAtDesc(Long orgId, Pageable pageable);

    Page<MemberTransfer> findByOrganizationIdAndChurchIdOrderByTransferredAtDesc(
        Long orgId, Long churchId, Pageable pageable);

    @Query("SELECT t FROM MemberTransfer t WHERE t.organizationId = :orgId " +
           "AND (t.fromCongregationId = :congId OR t.toCongregationId = :congId) " +
           "ORDER BY t.transferredAt DESC")
    Page<MemberTransfer> findByOrganizationIdAndCongregation(
        @Param("orgId") Long orgId, @Param("congId") Long congId, Pageable pageable);
}
