package com.igrejahub.finance.mapper;

import com.igrejahub.finance.dto.FinancialTransactionDto;
import com.igrejahub.finance.entity.FinancialAccount;
import com.igrejahub.finance.entity.FinancialCategory;
import com.igrejahub.finance.entity.FinancialTransaction;
import org.springframework.stereotype.Component;

@Component
public class FinancialTransactionMapper {

    public FinancialTransactionDto toDto(FinancialTransaction entity, FinancialAccount account, FinancialCategory category) {
        if (entity == null) return null;
        return FinancialTransactionDto.builder()
                .id(entity.getId())
                .churchId(entity.getChurchId())
                .congregationId(entity.getCongregationId())
                .accountId(entity.getAccountId())
                .accountName(account != null ? account.getName() : null)
                .categoryId(entity.getCategoryId())
                .categoryName(category != null ? category.getName() : null)
                .type(entity.getType().name())
                .description(entity.getDescription())
                .amount(FinancialAccountMapper.centsToAmount(entity.getAmountCents()))
                .transactionDate(entity.getTransactionDate())
                .status(entity.getStatus().name())
                .paymentMethod(entity.getPaymentMethod() != null ? entity.getPaymentMethod().name() : null)
                .reference(entity.getReference())
                .notes(entity.getNotes())
                .approvedBy(entity.getApprovedBy())
                .confirmedAt(entity.getConfirmedAt())
                .cancelledAt(entity.getCancelledAt())
                .build();
    }

    /** Overload simples, sem nomes de conta/categoria resolvidos (usado em listagens paginadas em lote). */
    public FinancialTransactionDto toDto(FinancialTransaction entity) {
        return toDto(entity, null, null);
    }
}