package com.igrejahub.accounting.repository;

import com.igrejahub.accounting.entity.JournalEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLine, Long> {
    List<JournalEntryLine> findByJournalEntryId(Long journalEntryId);

    @Query("SELECT COALESCE(SUM(l.debitCents), 0) - COALESCE(SUM(l.creditCents), 0) FROM JournalEntryLine l " +
           "JOIN l.journalEntry e WHERE l.accountId = :accountId AND e.organizationId = :orgId " +
           "AND e.status = 'POSTED' AND e.entryDate BETWEEN :startDate AND :endDate")
    Long sumBalanceByAccountAndPeriod(@Param("orgId") Long organizationId, @Param("accountId") Long accountId,
                                       @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}