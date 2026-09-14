package com.igrejahub.accounting.service;

import com.igrejahub.accounting.dto.ChartOfAccountDto;
import com.igrejahub.accounting.mapper.ChartOfAccountMapper;
import com.igrejahub.accounting.repository.ChartOfAccountRepository;
import com.igrejahub.accounting.repository.JournalEntryLineRepository;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.security.SecurityUtils;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountingReportService {

    private final ChartOfAccountRepository chartOfAccountRepository;
    private final ChartOfAccountMapper chartOfAccountMapper;
    private final JournalEntryLineRepository lineRepository;
    private final SecurityUtils securityUtils;

    public List<TrialBalanceLine> getTrialBalance(LocalDate startDate, LocalDate endDate) {
        Long orgId = TenantContext.getCurrentTenant();
        Long churchId = securityUtils.canViewAll() ? null : securityUtils.getEffectiveChurchId();
        List<ChartOfAccountDto> accounts = chartOfAccountRepository
                .findByOrganizationIdAndActiveTrueOrderByCode(orgId)
                .stream()
                .map(chartOfAccountMapper::toDto)
                .toList();

        if (!securityUtils.canViewAll() && churchId == null) {
            return List.of();
        }

        return accounts.stream()
                .map(a -> {
                    Long balance = lineRepository.sumBalanceByAccountAndPeriod(orgId, a.getId(), churchId, startDate, endDate);
                    return TrialBalanceLine.builder()
                            .accountId(a.getId())
                            .accountCode(a.getCode())
                            .accountName(a.getName())
                            .balanceCents(balance != null ? balance : 0L)
                            .build();
                })
                .toList();
    }

    @Data
    @Builder
    public static class TrialBalanceLine {
        private Long accountId;
        private String accountCode;
        private String accountName;
        private Long balanceCents;
    }
}