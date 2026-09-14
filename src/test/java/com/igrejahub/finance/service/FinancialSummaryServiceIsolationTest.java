package com.igrejahub.finance.service;

import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.finance.entity.FinancialTransaction;
import com.igrejahub.finance.repository.FinancialCategoryRepository;
import com.igrejahub.finance.repository.FinancialTransactionRepository;
import com.igrejahub.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialSummaryServiceIsolationTest {

    @Mock private FinancialTransactionRepository transactionRepository;
    @Mock private FinancialCategoryRepository categoryRepository;
    @Mock private SecurityUtils securityUtils;

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private FinancialSummaryService newService() {
        return new FinancialSummaryService(transactionRepository, categoryRepository, securityUtils);
    }

    @Test
    void getSummary_churchAdmin_filtersByOwnChurchOnly() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(null);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.isRoot()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(transactionRepository.sumConfirmedByCategoryAndPeriod(any(), any(), any(), any()))
            .thenReturn(List.of());

        newService().getSummary(LocalDate.now().minusDays(30), LocalDate.now());

        verify(transactionRepository).sumConfirmedRevenueCentsByFilter(
            eq(1L), any(), any(), eq(10L), isNull());
        verify(transactionRepository).sumConfirmedExpenseCentsByFilter(
            eq(1L), any(), any(), eq(10L), isNull());
    }

    @Test
    void getSummary_congregationPastor_filtersByOwnCongregation() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(100L);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.isRoot()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(transactionRepository.sumConfirmedByCategoryAndPeriod(any(), any(), any(), any()))
            .thenReturn(List.of());

        newService().getSummary(LocalDate.now().minusDays(30), LocalDate.now());

        verify(transactionRepository).sumConfirmedRevenueCentsByFilter(
            eq(1L), any(), any(), eq(10L), eq(100L));
        verify(transactionRepository).countByFilter(
            eq(1L), eq(FinancialTransaction.TransactionStatus.PENDING), eq(10L), eq(100L));
    }

    @Test
    void getSummary_rootGlobal_hasNoChurchFilter() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(null);
        when(securityUtils.canViewAll()).thenReturn(true);
        when(securityUtils.isRoot()).thenReturn(true);
        when(transactionRepository.sumConfirmedByCategoryAndPeriod(any(), any(), any(), any()))
            .thenReturn(List.of());

        newService().getSummary(LocalDate.now().minusDays(30), LocalDate.now());

        verify(transactionRepository).sumConfirmedRevenueCentsByFilter(
            eq(1L), any(), any(), isNull(), isNull());
    }
}
