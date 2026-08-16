package com.igrejahub.finance.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FinancialAccountDto {
    private Long id;
    private Long churchId;
    private String name;
    private String type;
    private String bankName;
    private String agency;
    private String accountNumber;
    private java.math.BigDecimal initialBalance;
    private java.math.BigDecimal currentBalance;
    private boolean active;
}