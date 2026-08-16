package com.igrejahub.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardFilterDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long churchId;
    private Long congregationId;
}
