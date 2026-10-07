package com.igrejahub.dashboard.service;

import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.dashboard.dto.DashboardFilterDto;
import com.igrejahub.dashboard.dto.DashboardMetrics;
import com.igrejahub.finance.mapper.FinancialAccountMapper;
import com.igrejahub.finance.repository.FinancialTransactionRepository;
import com.igrejahub.members.repository.MemberRepository;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.security.SecurityUtils;
import com.igrejahub.users.repository.UserRepository;
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
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;

    public DashboardMetrics getDashboardMetrics(DashboardFilterDto filter) {
        Long orgId = TenantContext.getCurrentTenant();

        LocalDate start = filter.getStartDate() != null ? filter.getStartDate() : LocalDate.now().withDayOfMonth(1);
        LocalDate end = filter.getEndDate() != null ? filter.getEndDate() : LocalDate.now();

        Long churchId;
        Long congregationId;
        boolean viewAll = securityUtils.canViewAll();

        if (viewAll) {
            // ROOT em modo global: livre para filtrar por qualquer Igreja/Congregação (ou nenhuma = org toda)
            churchId = filter.getChurchId();
            congregationId = filter.getCongregationId();
        } else {
            // Nunca confia em churchId/congregationId vindos do cliente — sempre
            // deriva do usuário autenticado (contexto/JWT).
            churchId = securityUtils.getEffectiveChurchId();
            Long ownCongId = TenantContext.getCurrentCongregationId();
            if (ownCongId != null) {
                congregationId = ownCongId; // restrito à própria congregação, sempre
            } else if (filter.getCongregationId() != null && churchId != null
                    && congregationRepository.existsByOrganizationIdAndIdAndChurchId(
                        orgId, filter.getCongregationId(), churchId)) {
                // Admin de Igreja pode opcionalmente "entrar" numa congregação específica da própria Igreja
                congregationId = filter.getCongregationId();
            } else {
                congregationId = null;
            }

            // Restrito às Congregações vinculadas e sem ter resolvido nenhuma
            // ainda → nenhum dado da Igreja inteira deve aparecer no painel.
            if (TenantContext.isMainChurchAccessDenied() && congregationId == null) {
                return DashboardMetrics.builder()
                    .totalMembers(0L).totalChurches(0L).totalCongregations(0L)
                    .monthlyRevenue(BigDecimal.ZERO).monthlyExpenses(BigDecimal.ZERO)
                    .balance(BigDecimal.ZERO).totalAssets(0L).activeUsers(0L)
                    .pendingTransactions(0L).pendingRevenue(BigDecimal.ZERO).pendingExpenses(BigDecimal.ZERO)
                    .build();
            }
        }

        long totalMembers = memberRepository.countByFilter(orgId, "ATIVO", churchId, congregationId);
        long totalChurches = viewAll
            ? churchRepository.countByOrganizationIdAndStatus(orgId, "ACTIVE")
            : 1L;
        long totalCongregations = viewAll
            ? congregationRepository.countByOrganizationIdAndStatus(orgId, "ACTIVE")
            : congregationId != null
                ? 1L
                : churchId != null ? congregationRepository.countByChurchId(churchId) : 0L;

        BigDecimal monthlyRevenue = FinancialAccountMapper.centsToAmount(
                transactionRepository.sumConfirmedRevenueCentsByFilter(orgId, start, end, churchId, congregationId));
        BigDecimal monthlyExpenses = FinancialAccountMapper.centsToAmount(
                transactionRepository.sumConfirmedExpenseCentsByFilter(orgId, start, end, churchId, congregationId));
        long pendingTransactions = transactionRepository.countByFilter(
                orgId, com.igrejahub.finance.entity.FinancialTransaction.TransactionStatus.PENDING, churchId, congregationId);

        // Lançado no período mas ainda não confirmado — só para avisar no
        // Dashboard; a regra de conciliação (só CONFIRMED entra em
        // monthlyRevenue/monthlyExpenses) não muda.
        BigDecimal pendingRevenue = FinancialAccountMapper.centsToAmount(
                transactionRepository.sumPendingRevenueCentsByFilter(orgId, start, end, churchId, congregationId));
        BigDecimal pendingExpenses = FinancialAccountMapper.centsToAmount(
                transactionRepository.sumPendingExpenseCentsByFilter(orgId, start, end, churchId, congregationId));

        long activeUsers = viewAll
            ? userRepository.countByOrganizationIdAndActive(orgId, true)
            : userRepository.countActiveByFilter(orgId, churchId, congregationId);

        return DashboardMetrics.builder()
                .totalMembers(totalMembers)
                .totalChurches(totalChurches)
                .totalCongregations(totalCongregations)
                .monthlyRevenue(monthlyRevenue)
                .monthlyExpenses(monthlyExpenses)
                .balance(monthlyRevenue.subtract(monthlyExpenses))
                .totalAssets(0L)
                .activeUsers(activeUsers)
                .pendingTransactions(pendingTransactions)
                .pendingRevenue(pendingRevenue)
                .pendingExpenses(pendingExpenses)
                .build();
    }
}