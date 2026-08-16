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
public class AssetDto {
    private Long id;
    private Long churchId;
    private String churchName;
    private Long congregationId;
    private String congregationName;
    private String code;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Long originalValueCents;
    private Double originalValue;
    private Long currentValueCents;
    private Double currentValue;
    private LocalDate acquisitionDate;
    private String location;
    private Long responsibleMemberId;
    private String responsibleMemberName;
    private Long responsibleUserId;
    private String responsibleUserName;
    private String status;
    private String notes;
    private String serialNumber;
    private String manufacturer;
    private String model;
    private LocalDate warrantyEndDate;
}
