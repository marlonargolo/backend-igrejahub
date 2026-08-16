package com.igrejahub.assets.entity;

import com.igrejahub.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "asset_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetCategory extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "depreciation_rate")
    private Double depreciationRate;

    @Column(name = "useful_life_years")
    private Integer usefulLifeYears;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_system")
    @Builder.Default
    private boolean system = false;
}
