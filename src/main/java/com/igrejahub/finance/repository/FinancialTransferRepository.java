package com.igrejahub.finance.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.finance.entity.FinancialTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialTransferRepository extends BaseRepository<FinancialTransfer, Long> {
    Page<FinancialTransfer> findByOrganizationId(Long organizationId, Pageable pageable);
}