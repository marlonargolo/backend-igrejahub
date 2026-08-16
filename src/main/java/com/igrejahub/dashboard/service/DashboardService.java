package com.igrejahub.dashboard.service;

import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.dashboard.dto.DashboardFilterDto;
import com.igrejahub.dashboard.dto.DashboardMetrics;
import com.igrejahub.finance.mapper.FinancialAccountMapper;
import com.igrejahub.finance.repository.FinancialTransactionRepository;
import com.igrejahub.members.repository.MemberRepository;
import com.igrejahub.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MemberRepository memberRepository;
    private final ChurchRepository churchRepository;
    private final CongregationRepository congregationRepository;
    private final FinancialTransactionRepository transactionRepository;

    public DashboardMetrics getDashboardMetrics(DashboardFilterDto filter) {
        Long orgId = TenantContext.getCurrentTenant();

        LocalDate start = filter.getStartDate() != null ? filter.getStartDate() : LocalDate.now().withDayOfMonth(1);
        LocalDate end = filter.getEndDate() != null ? filter.getEndDate() : LocalDate.now();
        Long churchId = filter.getChurchId();
        Long congregationId = filter.getCongregationId();

        long totalMembers = memberRepository.countByFilter(orgId, "ACTIVE", churchId, congregationId);
        long totalChurches = churchRepository.countByOrganizationIdAndStatus(orgId, "ACTIVE");
        long totalCongregations = congregationRepository.countByOrganizationIdAndStatus(orgId, "ACTIVE");

        BigDecimal monthlyRevenue = FinancialAccountMapper.centsToAmount(
                transactionRepository.sumConfirmedRevenueCentsByFilter(orgId, start, end, churchId, congregationId));
        BigDecimal monthlyExpenses = FinancialAccountMapper.centsToAmount(
                transactionRepository.sumConfirmedExpenseCentsByFilter(orgId, start, end, churchId, congregationId));
        long pendingTransactions = transactionRepository.countByFilter(
                orgId, com.igrejahub.finance.entity.FinancialTransaction.TransactionStatus.PENDING, churchId, congregationId);

        return DashboardMetrics.builder()
                .totalMembers(totalMembers)
                .totalChurches(totalChurches)
                .totalCongregations(totalCongregations)
                .monthlyRevenue(monthlyRevenue)
                .monthlyExpenses(monthlyExpenses)
                .balance(monthlyRevenue.subtract(monthlyExpenses))
                .totalAssets(0L)
                .activeUsers(1L)
                .pendingTransactions(pendingTransactions)
                .build();
    }
}