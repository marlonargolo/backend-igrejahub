package com.igrejahub.finance.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.finance.entity.FinancialCategory;
import com.igrejahub.finance.entity.FinancialTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Categorias visíveis: globais (church_id nulo) + as da Igreja efetiva.
     * viewAll=true (ROOT global) ignora o filtro de igreja.
     */
    @Query("SELECT c FROM FinancialCategory c WHERE c.organizationId = :orgId " +
           "AND (:viewAll = true OR c.churchId IS NULL OR c.churchId = :churchId)")
    Page<FinancialCategory> findVisible(@Param("orgId") Long orgId,
        @Param("viewAll") boolean viewAll, @Param("churchId") Long churchId, Pageable pageable);
}