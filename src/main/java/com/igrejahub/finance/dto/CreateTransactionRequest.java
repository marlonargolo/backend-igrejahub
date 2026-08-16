package com.igrejahub.finance.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateTransactionRequest {
    private Long churchId;
    private Long congregationId;
    @NotNull(message = "Conta é obrigatória")
    private Long accountId;
    private Long categoryId;
    @NotBlank(message = "Tipo é obrigatório (REVENUE ou EXPENSE)")
    private String type;
    @NotBlank(message = "Descrição é obrigatória")
    private String description;
    @NotNull @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal amount;
    @NotNull(message = "Data é obrigatória")
    private LocalDate transactionDate;
    private String paymentMethod;
    private String reference;
    private String notes;
}