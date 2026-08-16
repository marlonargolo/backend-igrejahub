package com.igrejahub.permissions.mapper;

import com.igrejahub.permissions.dto.PermissionDto;
import com.igrejahub.permissions.entity.Permission;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {
    public PermissionDto toDto(Permission entity) {
        if (entity == null) return null;
        PermissionDto dto = new PermissionDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory());
        dto.setActive(entity.isActive());
        dto.setSystem(entity.isSystem());
        return dto;
    }
    
    public Permission toEntity(PermissionDto dto) {
        if (dto == null) return null;
        Permission entity = new Permission();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setCategory(dto.getCategory());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);
        entity.setSystem(dto.getSystem() != null ? dto.getSystem() : false);
        return entity;
    }
}
