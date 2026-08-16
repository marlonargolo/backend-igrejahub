package com.igrejahub.congregations.mapper;

import com.igrejahub.congregations.dto.CongregationDto;
import com.igrejahub.congregations.entity.Congregation;
import org.springframework.stereotype.Component;

@Component
public class CongregationMapper {
    public CongregationDto toDto(Congregation entity) {
        if (entity == null) return null;
        CongregationDto dto = new CongregationDto();
        dto.setId(entity.getId());
        dto.setChurchId(entity.getChurchId());
        dto.setName(entity.getName());
        dto.setCity(entity.getCity());
        dto.setState(entity.getState());
        dto.setAddress(entity.getAddress());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
