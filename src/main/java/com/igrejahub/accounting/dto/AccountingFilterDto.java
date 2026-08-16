package com.igrejahub.accounting.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AccountingFilterDto {
    private Long churchId;
    private String status;
    private String accountType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String search;
}