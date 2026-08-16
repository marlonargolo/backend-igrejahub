package com.igrejahub.accounting.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class JournalEntryDto {
    private Long id;
    private String entryNumber;
    private LocalDate entryDate;
    private String description;
    private String status;
    private Long totalDebit;
    private Long totalCredit;
    private List<JournalEntryLineDto> lines;
}