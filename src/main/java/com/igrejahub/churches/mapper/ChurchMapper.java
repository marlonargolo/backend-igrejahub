package com.igrejahub.churches.mapper;

import com.igrejahub.churches.dto.ChurchDto;
import com.igrejahub.churches.entity.Church;
import org.springframework.stereotype.Component;

@Component
public class ChurchMapper {
    public ChurchDto toDto(Church entity) {
        if (entity == null) return null;
        return ChurchDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .city(entity.getCity())
                .state(entity.getState())
                .address(entity.getAddress())
                .zipCode(entity.getZipCode())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .cnpj(entity.getCnpj())
                .logoUrl(entity.getLogoUrl())
                .status(entity.getStatus())
                .pastorId(entity.getPastorId())
                .build();
    }
}