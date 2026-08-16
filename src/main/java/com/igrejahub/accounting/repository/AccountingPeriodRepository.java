package com.igrejahub.accounting.repository;

import com.igrejahub.accounting.entity.AccountingPeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {
    Page<AccountingPeriod> findByOrganizationId(Long organizationId, Pageable pageable);
    Optional<AccountingPeriod> findByOrganizationIdAndId(Long organizationId, Long id);

    @Query("SELECT p FROM AccountingPeriod p WHERE p.organizationId = :orgId " +
           "AND :date BETWEEN p.startDate AND p.endDate")
    Optional<AccountingPeriod> findByOrganizationIdAndDate(@Param("orgId") Long organizationId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(p) > 0 FROM AccountingPeriod p WHERE p.organizationId = :orgId " +
           "AND p.status = 'OPEN' AND ((p.startDate <= :endDate AND p.endDate >= :startDate))")
    boolean existsOverlappingOpenPeriod(@Param("orgId") Long organizationId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);
}