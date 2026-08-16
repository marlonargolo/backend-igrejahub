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
public class AssetMovementDto {
    private Long id;
    private Long assetId;
    private String assetDescription;
    private String fromLocation;
    private String toLocation;
    private LocalDate movementDate;
    private Long responsibleUserId;
    private String responsibleUserName;
    private String notes;
    private LocalDate createdAt;
}
