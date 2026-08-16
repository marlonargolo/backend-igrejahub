package com.igrejahub.finance.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FinancialSummaryDto {
    private BigDecimal totalRevenue;
    private BigDecimal totalExpense;
    private BigDecimal balance;
    private BigDecimal totalAssetsBalance;
    private long pendingTransactions;
    private List<CategoryBreakdown> revenueByCategory;
    private List<CategoryBreakdown> expenseByCategory;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoryBreakdown {
        private Long categoryId;
        private String categoryName;
        private BigDecimal amount;
    }
}