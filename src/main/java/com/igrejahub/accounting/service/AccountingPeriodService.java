package com.igrejahub.accounting.service;

import com.igrejahub.accounting.dto.AccountingPeriodDto;
import com.igrejahub.accounting.entity.AccountingPeriod;
import com.igrejahub.accounting.entity.AccountingPeriodStatus;
import com.igrejahub.accounting.mapper.AccountingPeriodMapper;
import com.igrejahub.accounting.repository.AccountingPeriodRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountingPeriodService {

    private final AccountingPeriodRepository periodRepository;
    private final AccountingPeriodMapper periodMapper;

    public Page<AccountingPeriodDto> getPeriods(Pageable pageable) {
        return periodRepository.findByOrganizationId(TenantContext.getCurrentTenant(), pageable)
                .map(periodMapper::toDto);
    }

    public AccountingPeriodDto getPeriodForDate(LocalDate date) {
        return periodRepository.findByOrganizationIdAndDate(TenantContext.getCurrentTenant(), date)
                .map(periodMapper::toDto)
                .orElseThrow(() -> new BusinessException("Nenhum período contábil aberto cobre a data " + date));
    }

    @Transactional
    public AccountingPeriodDto createPeriod(String name, LocalDate startDate, LocalDate endDate) {
        Long orgId = TenantContext.getCurrentTenant();
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("Data final não pode ser anterior à data inicial");
        }
        if (periodRepository.existsOverlappingOpenPeriod(orgId, startDate, endDate)) {
            throw new BusinessException("Já existe um período contábil aberto sobrepondo este intervalo");
        }
        AccountingPeriod period = new AccountingPeriod();
        period.setOrganizationId(orgId);
        period.setName(name);
        period.setStartDate(startDate);
        period.setEndDate(endDate);
        period.setStatus(AccountingPeriodStatus.OPEN.name());
        return periodMapper.toDto(periodRepository.save(period));
    }

    @Transactional
    public AccountingPeriodDto closePeriod(Long id) {
        AccountingPeriod period = getOwned(id);
        if (AccountingPeriodStatus.CLOSED.name().equals(period.getStatus())) {
            throw new BusinessException("Período já está fechado");
        }
        period.setStatus(AccountingPeriodStatus.CLOSED.name());
        return periodMapper.toDto(periodRepository.save(period));
    }

    @Transactional
    public AccountingPeriodDto reopenPeriod(Long id) {
        AccountingPeriod period = getOwned(id);
        period.setStatus(AccountingPeriodStatus.OPEN.name());
        return periodMapper.toDto(periodRepository.save(period));
    }

    private AccountingPeriod getOwned(Long id) {
        AccountingPeriod period = periodRepository.findByOrganizationIdAndId(TenantContext.getCurrentTenant(), id)
                .orElseThrow(() -> new ResourceNotFoundException("AccountingPeriod", id));
        return period;
    }
}