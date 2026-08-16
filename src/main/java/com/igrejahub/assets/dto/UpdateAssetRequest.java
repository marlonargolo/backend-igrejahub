package com.igrejahub.assets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAssetRequest {
    private Long churchId;
    private Long congregationId;
    private String code;
    private String description;
    private Long categoryId;
    private Double originalValue;
    private Double currentValue;
    private LocalDate acquisitionDate;
    private String location;
    private Long responsibleMemberId;
    private Long responsibleUserId;
    private String status;
    private String serialNumber;
    private String manufacturer;
    private String model;
    private LocalDate warrantyEndDate;
    private String notes;
}
