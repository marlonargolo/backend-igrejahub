package com.igrejahub.accounting.mapper;

import com.igrejahub.accounting.dto.JournalEntryDto;
import com.igrejahub.accounting.entity.JournalEntry;
import org.springframework.stereotype.Component;

@Component
public class JournalEntryMapper {
    public JournalEntryDto toDto(JournalEntry entity) {
        if (entity == null) return null;
        JournalEntryDto dto = new JournalEntryDto();
        dto.setId(entity.getId());
        dto.setEntryNumber(entity.getEntryNumber());
        dto.setEntryDate(entity.getEntryDate());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
