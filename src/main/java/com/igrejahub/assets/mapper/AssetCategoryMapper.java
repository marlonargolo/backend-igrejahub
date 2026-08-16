package com.igrejahub.assets.mapper;

import com.igrejahub.assets.dto.AssetCategoryDto;
import com.igrejahub.assets.entity.AssetCategory;
import org.springframework.stereotype.Component;

@Component
public class AssetCategoryMapper {
    public AssetCategoryDto toDto(AssetCategory entity) {
        if (entity == null) return null;
        AssetCategoryDto dto = new AssetCategoryDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.isActive());
        return dto;
    }
}
