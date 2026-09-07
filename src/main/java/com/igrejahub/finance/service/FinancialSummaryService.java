package com.igrejahub.finance.service;

import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.finance.dto.FinancialSummaryDto;
import com.igrejahub.finance.entity.FinancialTransaction;
import com.igrejahub.finance.mapper.FinancialAccountMapper;
import com.igrejahub.finance.repository.FinancialCategoryRepository;
import com.igrejahub.finance.repository.FinancialTransactionRepository;
import com.igrejahub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialSummaryService {

    private final FinancialTransactionRepository transactionRepository;
    private final FinancialCategoryRepository    categoryRepository;
    private final SecurityUtils                  securityUtils;

    public FinancialSummaryDto getSummary(LocalDate startDate, LocalDate endDate) {
        Long orgId    = TenantContext.getCurrentTenant();
        Long churchId = securityUtils.canViewAll() ? null : securityUtils.getEffectiveChurchId();
        Long congId   = !securityUtils.isRoot() ? TenantContext.getCurrentCongregationId() : null;

        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end   = endDate   != null ? endDate   : LocalDate.now();

        // Usar queries com filtro de churchId e congregationId
        long revenueCents = transactionRepository.sumConfirmedRevenueCentsByFilter(
            orgId, start, end, churchId, congId);
        long expenseCents = transactionRepository.sumConfirmedExpenseCentsByFilter(
            orgId, start, end, churchId, congId);
        long pending = transactionRepository.countByFilter(
            orgId, FinancialTransaction.TransactionStatus.PENDING, churchId, congId);

        BigDecimal revenue = FinancialAccountMapper.centsToAmount(revenueCents);
        BigDecimal expense = FinancialAccountMapper.centsToAmount(expenseCents);

        return FinancialSummaryDto.builder()
            .totalRevenue(revenue)
            .totalExpense(expense)
            .balance(revenue.subtract(expense))
            .totalAssetsBalance(BigDecimal.ZERO)
            .pendingTransactions(pending)
            .revenueByCategory(breakdown(orgId, FinancialTransaction.TransactionType.REVENUE, start, end))
            .expenseByCategory(breakdown(orgId, FinancialTransaction.TransactionType.EXPENSE, start, end))
            .build();
    }

    private List<FinancialSummaryDto.CategoryBreakdown> breakdown(Long orgId,
            FinancialTransaction.TransactionType type, LocalDate start, LocalDate end) {
        return transactionRepository.sumConfirmedByCategoryAndPeriod(orgId, type, start, end).stream()
            .map(row -> {
                Long categoryId = (Long) row[0];
                long cents = ((Number) row[1]).longValue();
                String name = categoryId != null
                    ? categoryRepository.findById(categoryId)
                        .map(c -> c.getName()).orElse("Sem categoria")
                    : "Sem categoria";
                return FinancialSummaryDto.CategoryBreakdown.builder()
                    .categoryId(categoryId).categoryName(name)
                    .amount(FinancialAccountMapper.centsToAmount(cents))
                    .build();
            }).toList();
    }
}