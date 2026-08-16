package com.igrejahub.accounting.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AccountingPeriodDto {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}
