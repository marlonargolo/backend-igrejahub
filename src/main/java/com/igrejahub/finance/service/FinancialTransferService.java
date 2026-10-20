package com.igrejahub.finance.service;

import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.finance.dto.FinancialTransferDto;
import com.igrejahub.finance.entity.FinancialAccount;
import com.igrejahub.finance.entity.FinancialTransfer;
import com.igrejahub.finance.mapper.FinancialAccountMapper;
import com.igrejahub.finance.mapper.FinancialTransferMapper;
import com.igrejahub.finance.repository.FinancialTransferRepository;
import com.igrejahub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialTransferService {

    private final FinancialTransferRepository transferRepository;
    private final FinancialAccountService accountService;
    private final FinancialTransferMapper transferMapper;
    private final SecurityUtils securityUtils;
    private final ChurchRepository churchRepository;

    public Page<FinancialTransferDto> getTransfers(Pageable pageable) {
        Long orgId = TenantContext.getCurrentTenant();

        Page<FinancialTransfer> page;
        if (securityUtils.canViewAll()) {
            page = transferRepository.findByOrganizationId(orgId, pageable);
        } else {
            Long churchId = securityUtils.getEffectiveChurchId();
            page = churchId == null
                ? Page.empty(pageable)
                : transferRepository.findByOrganizationIdAndChurchId(orgId, churchId, pageable);
        }
        return page.map(t -> transferMapper.toDto(t,
                accountService.getOwnedAccount(t.getFromAccountId()),
                accountService.getOwnedAccount(t.getToAccountId()),
                churchName(t.getChurchId()),
                churchName(t.getToChurchId())));
    }

    @Transactional
    public FinancialTransferDto createTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount,
                                                LocalDate transferDate, String description) {
        if (fromAccountId.equals(toAccountId)) {
            throw new BusinessException("Conta de origem e destino não podem ser iguais");
        }
        FinancialAccount from = accountService.getOwnedAccount(fromAccountId);
        FinancialAccount to = accountService.getOwnedAccount(toAccountId);

        // Transferência entre Igrejas diferentes: só quem enxerga todas as Igrejas
        // (ROOT em modo global) pode movimentar entre contas de Igrejas distintas.
        // Demais perfis já são barrados antes disso: getOwnedAccount() recusa
        // acesso a uma conta de outra Igreja.
        if (!Objects.equals(from.getChurchId(), to.getChurchId()) && !securityUtils.canViewAll()) {
            throw new BusinessException("Não é possível transferir entre contas de igrejas diferentes");
        }
        long cents = FinancialAccountMapper.amountToCents(amount);

        accountService.debitIfSufficient(from.getId(), cents);
        accountService.adjustBalance(to.getId(), cents);

        FinancialTransfer transfer = FinancialTransfer.builder()
                .churchId(from.getChurchId())
                .toChurchId(to.getChurchId())
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amountCents(cents)
                .transferDate(transferDate != null ? transferDate : LocalDate.now())
                .description(description)
                .status("CONFIRMED")
                .build();
        transfer.setOrganizationId(TenantContext.getCurrentTenant());
        transfer = transferRepository.save(transfer);
        return transferMapper.toDto(transfer, from, to, churchName(from.getChurchId()), churchName(to.getChurchId()));
    }

    private String churchName(Long churchId) {
        if (churchId == null) return null;
        return churchRepository.findById(churchId).map(c -> c.getName()).orElse(null);
    }
}