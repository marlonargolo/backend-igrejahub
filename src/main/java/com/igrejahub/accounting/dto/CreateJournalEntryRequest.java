package com.igrejahub.accounting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateJournalEntryRequest {
    private Long churchId;
    @NotNull(message = "Data do lançamento é obrigatória")
    private LocalDate entryDate;
    @NotBlank(message = "Descrição é obrigatória")
    private String description;
    private String reference;
    @NotEmpty(message = "É necessário informar ao menos 2 linhas (débito e crédito)")
    @Valid
    private List<JournalEntryLineDto> lines;
}