package com.igrejahub.assets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAssetCategoryRequest {
    private String name;
    private String description;
    private Long parentId;
    private Double depreciationRate;
    private Integer usefulLifeYears;
    private Boolean active;
}
