package com.igrejahub.assets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssetRequest {
    private Long churchId;
    private Long congregationId;
    private String code;

    @NotBlank(message = "Descrição é obrigatória")
    private String description;

    @NotNull(message = "Categoria é obrigatória")
    private Long categoryId;

    @NotNull(message = "Valor original é obrigatório")
    @Positive(message = "Valor deve ser positivo")
    private Double originalValue;

    @NotNull(message = "Data de aquisição é obrigatória")
    private LocalDate acquisitionDate;

    private String location;
    private Long responsibleMemberId;
    private Long responsibleUserId;
    private String serialNumber;
    private String manufacturer;
    private String model;
    private LocalDate warrantyEndDate;
    private String notes;
}
