package com.igrejahub.finance.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.finance.dto.FinancialAccountDto;
import com.igrejahub.finance.entity.FinancialAccount;
import com.igrejahub.finance.mapper.FinancialAccountMapper;
import com.igrejahub.finance.repository.FinancialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialAccountService {

    private final FinancialAccountRepository accountRepository;
    private final FinancialAccountMapper accountMapper;

    public Page<FinancialAccountDto> getAccounts(Pageable pageable) {
        return accountRepository.findByOrganizationId(TenantContext.getCurrentTenant(), pageable)
                .map(accountMapper::toDto);
    }

    public List<FinancialAccountDto> getActiveAccounts() {
        return accountRepository.findByOrganizationIdAndActiveTrue(TenantContext.getCurrentTenant())
                .stream().map(accountMapper::toDto).toList();
    }

    public FinancialAccountDto getAccount(Long id) {
        return accountMapper.toDto(getOwnedAccount(id));
    }

    @Transactional
    public FinancialAccountDto createAccount(String name, String type, String bankName, String agency,
                                              String accountNumber, BigDecimal initialBalance, Long churchId) {
        Long orgId = TenantContext.getCurrentTenant();
        Long initialCents = FinancialAccountMapper.amountToCents(initialBalance);
        FinancialAccount account = FinancialAccount.builder()
                .churchId(churchId)
                .name(name)
                .type(FinancialAccount.AccountType.valueOf(type))
                .bankName(bankName)
                .agency(agency)
                .accountNumber(accountNumber)
                .initialBalanceCents(initialCents)
                .currentBalanceCents(initialCents)
                .active(true)
                .build();
        account.setOrganizationId(orgId);
        return accountMapper.toDto(accountRepository.save(account));
    }

    @Transactional
    public FinancialAccountDto updateAccount(Long id, String name, String bankName, String agency,
                                              String accountNumber, Boolean active) {
        FinancialAccount account = getOwnedAccount(id);
        if (name != null) account.setName(name);
        if (bankName != null) account.setBankName(bankName);
        if (agency != null) account.setAgency(agency);
        if (accountNumber != null) account.setAccountNumber(accountNumber);
        if (active != null) account.setActive(active);
        return accountMapper.toDto(accountRepository.save(account));
    }

    @Transactional
    public void deleteAccount(Long id) {
        FinancialAccount account = getOwnedAccount(id);
        account.setActive(false);
        accountRepository.save(account);
    }

    /** Usado pelo FinancialTransactionService/TransferService para creditar/debitar saldo. */
    @Transactional
    public void adjustBalance(Long accountId, long deltaCents) {
        accountRepository.adjustBalance(accountId, deltaCents);
    }

    /** Debita atomicamente só se houver saldo suficiente, evitando corrida entre checagem e ajuste. */
    @Transactional
    public void debitIfSufficient(Long accountId, long cents) {
        int updated = accountRepository.debitIfSufficient(accountId, cents);
        if (updated == 0) {
            throw new BusinessException("Saldo insuficiente na conta de origem");
        }
    }

    FinancialAccount getOwnedAccount(Long id) {
        FinancialAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialAccount", id));
        if (!account.getOrganizationId().equals(TenantContext.getCurrentTenant())) {
            throw new BusinessException("Acesso não autorizado");
        }
        return account;
    }
}