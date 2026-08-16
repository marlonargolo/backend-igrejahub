package com.igrejahub.finance.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FinancialCategoryDto {
    private Long id;
    private String name;
    private String type;
    private String color;
    private boolean active;
}