package com.igrejahub.assets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetCategoryDto {
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private String parentName;
    private Double depreciationRate;
    private Integer usefulLifeYears;
    private Boolean active;
    private Boolean system;
}
