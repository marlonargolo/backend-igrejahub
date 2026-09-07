package com.igrejahub.finance.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FinancialTransactionDto {
    private Long id;
    private Long churchId;
    private Long congregationId;
    private Long memberId;          // NOVO
    private String memberName;      // NOVO — nome do membro vinculado
    private Long accountId;
    private String accountName;
    private Long categoryId;
    private String categoryName;
    private String type;
    private String description;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String status;
    private String paymentMethod;
    private String reference;
    private String notes;
    private Long approvedBy;
    private LocalDate confirmedAt;
    private LocalDate cancelledAt;
}