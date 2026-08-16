package com.igrejahub.accounting.mapper;

import com.igrejahub.accounting.dto.JournalEntryLineDto;
import com.igrejahub.accounting.entity.JournalEntryLine;
import org.springframework.stereotype.Component;

@Component
public class JournalEntryLineMapper {
    public JournalEntryLineDto toDto(JournalEntryLine entity) {
        if (entity == null) return null;
        JournalEntryLineDto dto = new JournalEntryLineDto();
        dto.setId(entity.getId());
        dto.setAccountId(entity.getAccountId());
        dto.setDebitCents(entity.getDebitCents());
        dto.setCreditCents(entity.getCreditCents());
        dto.setDescription(entity.getDescription());
        return dto;
    }
}
