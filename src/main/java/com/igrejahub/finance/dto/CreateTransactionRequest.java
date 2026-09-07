package com.igrejahub.finance.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateTransactionRequest {
    private Long churchId;
    private Long congregationId;
    private Long memberId;      // NOVO — vínculo de contribuição ao membro
    private Long accountId;
    private Long categoryId;
    private String type;
    private String description;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String paymentMethod;
    private String reference;
    private String notes;
}