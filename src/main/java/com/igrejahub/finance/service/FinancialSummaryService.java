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
import java.util.ArrayList;
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
        Long congId   = TenantContext.getCurrentCongregationId();

        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end   = endDate   != null ? endDate   : LocalDate.now();

        long revenueCents = transactionRepository.sumConfirmedRevenueCentsByFilter(
            orgId, start, end, churchId, congId);
        long expenseCents = transactionRepository.sumConfirmedExpenseCentsByFilter(
            orgId, start, end, churchId, congId);
        long pending = transactionRepository.countByFilter(
            orgId, FinancialTransaction.TransactionStatus.PENDING, churchId, congId);

        BigDecimal revenue = FinancialAccountMapper.centsToAmount(revenueCents);
        BigDecimal expense = FinancialAccountMapper.centsToAmount(expenseCents);

        List<FinancialSummaryDto.CategoryBreakdown> revenueBreakdown =
            buildBreakdown(orgId, FinancialTransaction.TransactionType.REVENUE, start, end);
        List<FinancialSummaryDto.CategoryBreakdown> expenseBreakdown =
            buildBreakdown(orgId, FinancialTransaction.TransactionType.EXPENSE, start, end);

        return new FinancialSummaryDto(
            revenue,
            expense,
            revenue.subtract(expense),
            BigDecimal.ZERO,
            pending,
            revenueBreakdown,
            expenseBreakdown
        );
    }

    private List<FinancialSummaryDto.CategoryBreakdown> buildBreakdown(Long orgId,
            FinancialTransaction.TransactionType type, LocalDate start, LocalDate end) {
        List<Object[]> rows = transactionRepository.sumConfirmedByCategoryAndPeriod(orgId, type, start, end);
        List<FinancialSummaryDto.CategoryBreakdown> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long categoryId = (Long) row[0];
            long cents = ((Number) row[1]).longValue();
            String name = "Sem categoria";
            if (categoryId != null) {
                name = categoryRepository.findById(categoryId)
                    .map(c -> c.getName()).orElse("Sem categoria");
            }
            result.add(new FinancialSummaryDto.CategoryBreakdown(
                categoryId, name, FinancialAccountMapper.centsToAmount(cents)));
        }
        return result;
    }
}