package com.igrejahub.plans.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PlanDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private int maxUsers;
    private int maxCongregations;
    private int maxMembers;
    private String features;
    private boolean active;
    private boolean system;
}