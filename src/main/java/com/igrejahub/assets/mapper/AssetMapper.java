package com.igrejahub.assets.mapper;

import com.igrejahub.assets.dto.AssetDto;
import com.igrejahub.assets.entity.Asset;
import org.springframework.stereotype.Component;

@Component
public class AssetMapper {
    public AssetDto toDto(Asset entity) {
        if (entity == null) return null;
        AssetDto dto = new AssetDto();
        dto.setId(entity.getId());
        dto.setDescription(entity.getDescription());
        dto.setCode(entity.getCode());
        dto.setStatus(entity.getStatus());
        dto.setLocation(entity.getLocation());
        return dto;
    }
}
