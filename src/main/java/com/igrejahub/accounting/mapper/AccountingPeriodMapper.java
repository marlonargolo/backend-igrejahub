package com.igrejahub.accounting.mapper;

import com.igrejahub.accounting.dto.AccountingPeriodDto;
import com.igrejahub.accounting.entity.AccountingPeriod;
import org.springframework.stereotype.Component;

@Component
public class AccountingPeriodMapper {
    public AccountingPeriodDto toDto(AccountingPeriod entity) {
        if (entity == null) return null;
        AccountingPeriodDto dto = new AccountingPeriodDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
