package com.igrejahub.accounting.mapper;

import com.igrejahub.accounting.dto.ChartOfAccountDto;
import com.igrejahub.accounting.entity.ChartOfAccount;
import org.springframework.stereotype.Component;

@Component
public class ChartOfAccountMapper {
    public ChartOfAccountDto toDto(ChartOfAccount entity) {
        if (entity == null) return null;
        ChartOfAccountDto dto = new ChartOfAccountDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setAccountType(entity.getAccountType());
        return dto;
    }
}
