package com.igrejahub.accounting.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateJournalEntryRequest {
    private LocalDate entryDate;
    private String description;
    private String reference;
    @Valid
    private List<JournalEntryLineDto> lines;
}