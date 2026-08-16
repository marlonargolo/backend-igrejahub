package com.igrejahub.accounting.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class AccountingExportDto {
    private String format; // CSV, PDF, XLSX
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Long> accountIds;
}