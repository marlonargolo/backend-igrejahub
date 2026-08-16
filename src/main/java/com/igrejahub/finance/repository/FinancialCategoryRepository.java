package com.igrejahub.finance.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.finance.entity.FinancialCategory;
import com.igrejahub.finance.entity.FinancialTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialCategoryRepository extends BaseRepository<FinancialCategory, Long> {
    Page<FinancialCategory> findByOrganizationId(Long organizationId, Pageable pageable);
    List<FinancialCategory> findByOrganizationIdAndActiveTrue(Long organizationId);
    List<FinancialCategory> findByOrganizationIdAndType(Long organizationId, FinancialTransaction.TransactionType type);
    boolean existsByOrganizationIdAndNameIgnoreCase(Long organizationId, String name);
    Optional<FinancialCategory> findByIdAndOrganizationId(Long id, Long organizationId);
}