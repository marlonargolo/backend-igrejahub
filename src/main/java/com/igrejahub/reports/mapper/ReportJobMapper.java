package com.igrejahub.reports.mapper;

import com.igrejahub.reports.dto.ReportJobDto;
import com.igrejahub.reports.entity.ReportJob;
import org.springframework.stereotype.Component;

@Component
public class ReportJobMapper {
    public ReportJobDto toDto(ReportJob entity) {
        if (entity == null) return null;
        ReportJobDto dto = new ReportJobDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setFormat(entity.getFormat());
        dto.setStatus(entity.getStatus());
        dto.setFilters(entity.getFilters());
        dto.setFileUrl(entity.getFileUrl());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setExpiresAt(entity.getExpiresAt());
        return dto;
    }
}
