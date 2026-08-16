package com.igrejahub.roles.mapper;

import com.igrejahub.roles.dto.RoleDto;
import com.igrejahub.roles.entity.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {
    public RoleDto toDto(Role entity) {
        if (entity == null) return null;
        RoleDto dto = new RoleDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.isActive());
        dto.setSystem(entity.isSystem());
        return dto;
    }
}
