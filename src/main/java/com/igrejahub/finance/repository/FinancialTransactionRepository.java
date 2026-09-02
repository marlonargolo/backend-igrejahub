package com.igrejahub.finance.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.finance.entity.FinancialTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FinancialTransactionRepository extends BaseRepository<FinancialTransaction, Long> {

    // ── Existentes — não remover ──────────────────────────────────────────────
    Page<FinancialTransaction> findByOrganizationId(Long organizationId, Pageable pageable);
    Page<FinancialTransaction> findByOrganizationIdAndStatus(
        Long organizationId, FinancialTransaction.TransactionStatus status, Pageable pageable);
    long countByOrganizationIdAndStatus(
        Long organizationId, FinancialTransaction.TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amountCents), 0) FROM FinancialTransaction t " +
           "WHERE t.organizationId = :orgId AND t.type = 'REVENUE' AND t.status = 'CONFIRMED' " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    long sumConfirmedRevenueCentsByOrganizationAndPeriod(
        @Param("orgId") Long organizationId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amountCents), 0) FROM FinancialTransaction t " +
           "WHERE t.organizationId = :orgId AND t.type = 'EXPENSE' AND t.status = 'CONFIRMED' " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    long sumConfirmedExpenseCentsByOrganizationAndPeriod(
        @Param("orgId") Long organizationId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amountCents), 0) FROM FinancialTransaction t " +
           "WHERE t.organizationId = :orgId AND t.type = 'REVENUE' AND t.status = 'CONFIRMED' " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "AND (:churchId IS NULL OR t.churchId = :churchId) " +
           "AND (:congregationId IS NULL OR t.congregationId = :congregationId)")
    long sumConfirmedRevenueCentsByFilter(
        @Param("orgId") Long organizationId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("churchId") Long churchId,
        @Param("congregationId") Long congregationId);

    @Query("SELECT COALESCE(SUM(t.amountCents), 0) FROM FinancialTransaction t " +
           "WHERE t.organizationId = :orgId AND t.type = 'EXPENSE' AND t.status = 'CONFIRMED' " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "AND (:churchId IS NULL OR t.churchId = :churchId) " +
           "AND (:congregationId IS NULL OR t.congregationId = :congregationId)")
    long sumConfirmedExpenseCentsByFilter(
        @Param("orgId") Long organizationId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("churchId") Long churchId,
        @Param("congregationId") Long congregationId);

    @Query("SELECT COUNT(t) FROM FinancialTransaction t " +
           "WHERE t.organizationId = :orgId AND t.status = :status " +
           "AND (:churchId IS NULL OR t.churchId = :churchId) " +
           "AND (:congregationId IS NULL OR t.congregationId = :congregationId)")
    long countByFilter(
        @Param("orgId") Long organizationId,
        @Param("status") FinancialTransaction.TransactionStatus status,
        @Param("churchId") Long churchId,
        @Param("congregationId") Long congregationId);

    @Query("SELECT t.categoryId, COALESCE(SUM(t.amountCents), 0) FROM FinancialTransaction t " +
           "WHERE t.organizationId = :orgId AND t.type = :type AND t.status = 'CONFIRMED' " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate GROUP BY t.categoryId")
    List<Object[]> sumConfirmedByCategoryAndPeriod(
        @Param("orgId") Long organizationId,
        @Param("type") FinancialTransaction.TransactionType type,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    // ── Novos: filtro por Igreja (churchId OBRIGATÓRIO) ───────────────────────

    @Query("SELECT t FROM FinancialTransaction t WHERE t.organizationId = :orgId " +
           "AND t.deleted = false AND t.churchId = :churchId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "ORDER BY t.transactionDate DESC")
    Page<FinancialTransaction> findByChurchId(
        @Param("orgId") Long orgId,
        @Param("churchId") Long churchId,
        @Param("status") FinancialTransaction.TransactionStatus status,
        Pageable pageable);

    // Por congregação (PASTOR_CONGREGACAO)
    @Query("SELECT t FROM FinancialTransaction t WHERE t.organizationId = :orgId " +
           "AND t.deleted = false AND t.congregationId = :congregationId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "ORDER BY t.transactionDate DESC")
    Page<FinancialTransaction> findByCongregationId(
        @Param("orgId") Long orgId,
        @Param("congregationId") Long congregationId,
        @Param("status") FinancialTransaction.TransactionStatus status,
        Pageable pageable);
}