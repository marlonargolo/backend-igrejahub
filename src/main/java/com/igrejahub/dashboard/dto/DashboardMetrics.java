package com.igrejahub.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetrics {
    private Long totalMembers;
    private Long totalChurches;
    private Long totalCongregations;
    private BigDecimal monthlyRevenue;
    private BigDecimal monthlyExpenses;
    private BigDecimal balance;
    private Long totalAssets;
    private Long activeUsers;
    private Long pendingTransactions;
}
