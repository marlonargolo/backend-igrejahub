package com.igrejahub.finance.mapper;

import com.igrejahub.finance.dto.FinancialCategoryDto;
import com.igrejahub.finance.entity.FinancialCategory;
import org.springframework.stereotype.Component;

@Component
public class FinancialCategoryMapper {
    public FinancialCategoryDto toDto(FinancialCategory entity) {
        if (entity == null) return null;
        return FinancialCategoryDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType().name())
                .color(entity.getColor())
                .active(entity.isActive())
                .build();
    }
}