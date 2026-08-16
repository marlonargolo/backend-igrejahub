package com.igrejahub.accounting.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JournalEntryLineDto {
    private Long id;
    @NotNull(message = "Conta contábil é obrigatória")
    private Long accountId;
    @NotNull
    private Long debitCents = 0L;
    @NotNull
    private Long creditCents = 0L;
    private String description;
}