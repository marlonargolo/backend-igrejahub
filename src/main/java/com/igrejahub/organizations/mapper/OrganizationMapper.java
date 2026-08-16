package com.igrejahub.organizations.mapper;

import com.igrejahub.organizations.dto.OrganizationDto;
import com.igrejahub.organizations.entity.Organization;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {
    public OrganizationDto toDto(Organization entity) {
        if (entity == null) return null;
        OrganizationDto dto = new OrganizationDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setEmail(entity.getEmail());
        dto.setCnpj(entity.getCnpj());
        dto.setPhone(entity.getPhone());
        dto.setAddress(entity.getAddress());
        dto.setCity(entity.getCity());
        dto.setState(entity.getState());
        dto.setZipCode(entity.getZipCode());
        dto.setCountry(entity.getCountry());
        dto.setLogoUrl(entity.getLogoUrl());
        dto.setPlan(entity.getPlan());
        dto.setActive(entity.isActive());
        return dto;
    }
}
