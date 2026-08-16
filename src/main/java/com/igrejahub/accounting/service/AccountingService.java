package com.igrejahub.accounting.service;

import com.igrejahub.common.tenant.TenantContext;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountingService {

    private final AccountingReportService reportService;

    public AccountingOverview getOverview() {
        Long orgId = TenantContext.getCurrentTenant();
        LocalDate today = LocalDate.now();
        var trialBalance = reportService.getTrialBalance(today.withDayOfMonth(1), today);
        long totalDebit = trialBalance.stream()
                .mapToLong(l -> l.getBalanceCents() != null && l.getBalanceCents() > 0 ? l.getBalanceCents() : 0)
                .sum();
        return AccountingOverview.builder()
                .organizationId(orgId)
                .accountsCount(trialBalance.size())
                .totalDebitCents(totalDebit)
                .build();
    }

    @Data
    @Builder
    public static class AccountingOverview {
        private Long organizationId;
        private int accountsCount;
        private long totalDebitCents;
    }
}