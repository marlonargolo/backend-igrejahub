package com.igrejahub.accounting.service;

import com.igrejahub.accounting.dto.ChartOfAccountDto;
import com.igrejahub.accounting.entity.ChartOfAccount;
import com.igrejahub.accounting.mapper.ChartOfAccountMapper;
import com.igrejahub.accounting.repository.ChartOfAccountRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChartOfAccountService {

    private final ChartOfAccountRepository accountRepository;
    private final ChartOfAccountMapper accountMapper;

    public List<ChartOfAccountDto> getAccounts() {
        return accountRepository.findByOrganizationIdAndActiveTrueOrderByCode(TenantContext.getCurrentTenant())
                .stream().map(accountMapper::toDto).toList();
    }

    public ChartOfAccountDto getAccount(Long id) {
        return accountMapper.toDto(getOwned(id));
    }

    @Transactional
    public ChartOfAccountDto createAccount(String code, String name, String accountType,
                                            Long parentId, String normalBalance, boolean analytical) {
        Long orgId = TenantContext.getCurrentTenant();
        if (accountRepository.existsByOrganizationIdAndCode(orgId, code)) {
            throw new BusinessException("Já existe uma conta com este código no plano de contas");
        }
        int level = 1;
        if (parentId != null) {
            ChartOfAccount parent = getOwned(parentId);
            level = parent.getLevel() + 1;
        }
        ChartOfAccount account = new ChartOfAccount();
        account.setOrganizationId(orgId);
        account.setCode(code);
        account.setName(name);
        account.setAccountType(accountType);
        account.setParentId(parentId);
        account.setLevel(level);
        account.setNormalBalance(normalBalance);
        account.setAnalytical(analytical);
        account.setActive(true);
        return accountMapper.toDto(accountRepository.save(account));
    }

    @Transactional
    public ChartOfAccountDto updateAccount(Long id, String name, Boolean active) {
        ChartOfAccount account = getOwned(id);
        if (name != null) account.setName(name);
        if (active != null) account.setActive(active);
        return accountMapper.toDto(accountRepository.save(account));
    }

    @Transactional
    public void deleteAccount(Long id) {
        ChartOfAccount account = getOwned(id);
        if (accountRepository.existsByParentId(id)) {
            throw new BusinessException("Não é possível excluir uma conta que possui subcontas");
        }
        account.setActive(false);
        accountRepository.save(account);
    }

    private ChartOfAccount getOwned(Long id) {
        ChartOfAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChartOfAccount", id));
        if (!account.getOrganizationId().equals(TenantContext.getCurrentTenant())) {
            throw new BusinessException("Acesso não autorizado");
        }
        return account;
    }
}