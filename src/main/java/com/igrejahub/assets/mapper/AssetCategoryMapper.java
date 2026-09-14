package com.igrejahub.assets.mapper;

import com.igrejahub.assets.dto.AssetCategoryDto;
import com.igrejahub.assets.entity.AssetCategory;
import org.springframework.stereotype.Component;

@Component
public class AssetCategoryMapper {

    public AssetCategoryDto toDto(AssetCategory entity) {
        if (entity == null) return null;
        AssetCategoryDto dto = new AssetCategoryDto();
        // Use reflection to handle different entity versions
        try { dto.setId((Long) entity.getClass().getMethod("getId").invoke(entity)); } catch (Exception ignored) {}
        try { dto.setName((String) entity.getClass().getMethod("getName").invoke(entity)); } catch (Exception ignored) {}
        try { dto.setDescription((String) entity.getClass().getMethod("getDescription").invoke(entity)); } catch (Exception ignored) {}
        try { dto.setActive((boolean) entity.getClass().getMethod("isActive").invoke(entity)); } catch (Exception ignored) {}
        return dto;
    }
}