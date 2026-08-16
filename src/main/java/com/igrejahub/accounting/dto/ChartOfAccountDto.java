package com.igrejahub.accounting.dto;

import lombok.Data;

@Data
public class ChartOfAccountDto {
    private Long id;
    private String code;
    private String name;
    private String accountType;
}
