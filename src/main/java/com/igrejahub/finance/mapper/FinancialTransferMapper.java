package com.igrejahub.finance.mapper;

import com.igrejahub.finance.dto.FinancialTransferDto;
import com.igrejahub.finance.entity.FinancialAccount;
import com.igrejahub.finance.entity.FinancialTransfer;
import org.springframework.stereotype.Component;

@Component
public class FinancialTransferMapper {
    public FinancialTransferDto toDto(FinancialTransfer entity, FinancialAccount from, FinancialAccount to) {
        return toDto(entity, from, to, null, null);
    }

    public FinancialTransferDto toDto(FinancialTransfer entity, FinancialAccount from, FinancialAccount to,
                                       String fromChurchName, String toChurchName) {
        if (entity == null) return null;
        return FinancialTransferDto.builder()
                .id(entity.getId())
                .fromAccountId(entity.getFromAccountId())
                .fromAccountName(from != null ? from.getName() : null)
                .fromChurchId(entity.getChurchId())
                .fromChurchName(fromChurchName)
                .toAccountId(entity.getToAccountId())
                .toAccountName(to != null ? to.getName() : null)
                .toChurchId(entity.getToChurchId())
                .toChurchName(toChurchName)
                .amount(FinancialAccountMapper.centsToAmount(entity.getAmountCents()))
                .transferDate(entity.getTransferDate())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .build();
    }
}