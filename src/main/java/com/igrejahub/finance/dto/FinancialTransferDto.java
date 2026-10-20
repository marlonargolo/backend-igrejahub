package com.igrejahub.finance.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FinancialTransferDto {
    private Long id;
    private Long fromAccountId;
    private String fromAccountName;
    private Long fromChurchId;
    private String fromChurchName;
    private Long toAccountId;
    private String toAccountName;
    private Long toChurchId;
    private String toChurchName;
    private BigDecimal amount;
    private LocalDate transferDate;
    private String description;
    private String status;
}