package com.igrejahub.users.mapper;

import com.igrejahub.users.dto.UserDto;
import com.igrejahub.users.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDto toDto(User entity) {
        if (entity == null) return null;
        return UserDto.builder()
            .id(entity.getId())
            .name(entity.getName())
            .email(entity.getEmail())
            .phone(entity.getPhone())
            .organizationId(entity.getOrganizationId())
            .active(entity.isActive())
            .verified(entity.isVerified())
            .lastLoginAt(entity.getLastLoginAt())
            .roles(entity.getRoles() != null
                ? entity.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet())
                : null)
            .permissions(entity.getRoles() != null
                ? entity.getRoles().stream()
                    .flatMap(r -> r.getPermissions().stream())
                    .map(p -> p.getName())
                    .collect(Collectors.toSet())
                : null)
            .build();
    }
}
