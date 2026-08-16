package com.igrejahub.finance.mapper;

import com.igrejahub.finance.dto.FinancialAccountDto;
import com.igrejahub.finance.entity.FinancialAccount;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class FinancialAccountMapper {
    public FinancialAccountDto toDto(FinancialAccount entity) {
        if (entity == null) return null;
        return FinancialAccountDto.builder()
                .id(entity.getId())
                .churchId(entity.getChurchId())
                .name(entity.getName())
                .type(entity.getType().name())
                .bankName(entity.getBankName())
                .agency(entity.getAgency())
                .accountNumber(entity.getAccountNumber())
                .initialBalance(centsToAmount(entity.getInitialBalanceCents()))
                .currentBalance(centsToAmount(entity.getCurrentBalanceCents()))
                .active(entity.isActive())
                .build();
    }

    public static BigDecimal centsToAmount(Long cents) {
        return cents == null ? BigDecimal.ZERO : BigDecimal.valueOf(cents, 2);
    }

    public static Long amountToCents(BigDecimal amount) {
        return amount == null ? 0L : amount.movePointRight(2).longValueExact();
    }
}