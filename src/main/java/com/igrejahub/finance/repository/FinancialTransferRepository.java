package com.igrejahub.finance.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.finance.entity.FinancialTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialTransferRepository extends BaseRepository<FinancialTransfer, Long> {
    Page<FinancialTransfer> findByOrganizationId(Long organizationId, Pageable pageable);

    /** Visível para a Igreja tanto como origem quanto como destino da transferência. */
    @Query("SELECT t FROM FinancialTransfer t WHERE t.organizationId = :organizationId " +
           "AND (t.churchId = :churchId OR t.toChurchId = :churchId)")
    Page<FinancialTransfer> findByOrganizationIdAndChurchId(
            @Param("organizationId") Long organizationId, @Param("churchId") Long churchId, Pageable pageable);
}