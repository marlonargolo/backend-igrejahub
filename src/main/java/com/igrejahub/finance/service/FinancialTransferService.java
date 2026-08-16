package com.igrejahub.finance.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.finance.dto.FinancialTransferDto;
import com.igrejahub.finance.entity.FinancialAccount;
import com.igrejahub.finance.entity.FinancialTransfer;
import com.igrejahub.finance.mapper.FinancialAccountMapper;
import com.igrejahub.finance.mapper.FinancialTransferMapper;
import com.igrejahub.finance.repository.FinancialTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialTransferService {

    private final FinancialTransferRepository transferRepository;
    private final FinancialAccountService accountService;
    private final FinancialTransferMapper transferMapper;

    public Page<FinancialTransferDto> getTransfers(Pageable pageable) {
        return transferRepository.findByOrganizationId(TenantContext.getCurrentTenant(), pageable)
                .map(t -> transferMapper.toDto(t,
                        accountService.getOwnedAccount(t.getFromAccountId()),
                        accountService.getOwnedAccount(t.getToAccountId())));
    }

    @Transactional
    public FinancialTransferDto createTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount,
                                                LocalDate transferDate, String description) {
        if (fromAccountId.equals(toAccountId)) {
            throw new BusinessException("Conta de origem e destino não podem ser iguais");
        }
        FinancialAccount from = accountService.getOwnedAccount(fromAccountId);
        FinancialAccount to = accountService.getOwnedAccount(toAccountId);
        long cents = FinancialAccountMapper.amountToCents(amount);

        accountService.debitIfSufficient(from.getId(), cents);
        accountService.adjustBalance(to.getId(), cents);

        FinancialTransfer transfer = FinancialTransfer.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amountCents(cents)
                .transferDate(transferDate != null ? transferDate : LocalDate.now())
                .description(description)
                .status("CONFIRMED")
                .build();
        transfer.setOrganizationId(TenantContext.getCurrentTenant());
        transfer = transferRepository.save(transfer);
        return transferMapper.toDto(transfer, from, to);
    }
}