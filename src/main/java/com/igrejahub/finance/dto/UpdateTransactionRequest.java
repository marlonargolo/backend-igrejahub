package com.igrejahub.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateTransactionRequest {
    private Long accountId;
    private Long categoryId;
    private String description;
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String paymentMethod;
    private String reference;
    private String notes;
}