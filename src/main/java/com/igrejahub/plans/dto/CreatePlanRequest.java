package com.igrejahub.plans.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreatePlanRequest {

    @NotBlank(message = "Nome do plano é obrigatório")
    private String name;

    private String description;

    @NotNull @DecimalMin("0.00")
    private BigDecimal price;

    @NotNull @Min(1) @Max(9999)
    private Integer maxUsers;

    @NotNull @Min(1) @Max(9999)
    private Integer maxCongregations;

    @NotNull @Min(1)
    private Integer maxMembers;

    private String features;
}