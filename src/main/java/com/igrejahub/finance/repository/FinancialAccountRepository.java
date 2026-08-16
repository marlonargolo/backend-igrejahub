package com.igrejahub.finance.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.finance.entity.FinancialAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FinancialAccountRepository extends BaseRepository<FinancialAccount, Long> {
    Page<FinancialAccount> findByOrganizationId(Long organizationId, Pageable pageable);
    List<FinancialAccount> findByOrganizationIdAndActiveTrue(Long organizationId);

    @Modifying
    @Query("UPDATE FinancialAccount a SET a.currentBalanceCents = a.currentBalanceCents + :deltaCents " +
           "WHERE a.id = :accountId")
    void adjustBalance(@Param("accountId") Long accountId, @Param("deltaCents") Long deltaCents);

    @Modifying
    @Query("UPDATE FinancialAccount a SET a.currentBalanceCents = a.currentBalanceCents - :cents " +
           "WHERE a.id = :accountId AND a.currentBalanceCents >= :cents")
    int debitIfSufficient(@Param("accountId") Long accountId, @Param("cents") Long cents);
}