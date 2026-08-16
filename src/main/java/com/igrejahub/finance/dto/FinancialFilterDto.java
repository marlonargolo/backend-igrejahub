package com.igrejahub.finance.dto;

import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FinancialFilterDto {
    private Long churchId;
    private Long congregationId;
    private Long accountId;
    private Long categoryId;
    private String type;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String search;
}