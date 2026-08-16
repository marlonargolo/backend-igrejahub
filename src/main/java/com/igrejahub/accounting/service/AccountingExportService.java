package com.igrejahub.accounting.service;

import com.igrejahub.accounting.dto.AccountingExportDto;
import com.igrejahub.accounting.dto.JournalEntryDto;
import com.igrejahub.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountingExportService {

    private final JournalEntryService journalEntryService;

    public byte[] export(AccountingExportDto request) {
        String format = request.getFormat() != null ? request.getFormat().toUpperCase() : "CSV";
        List<JournalEntryDto> entries = journalEntryService
                .getEntries(PageRequest.of(0, 1000), "POSTED")
                .getContent();

        return switch (format) {
            case "CSV" -> toCsv(entries);
            default -> throw new BusinessException("Formato de exportação não suportado ainda: " + format);
        };
    }

    private byte[] toCsv(List<JournalEntryDto> entries) {
        StringBuilder sb = new StringBuilder("numero,data,descricao,status,debito,credito\n");
        for (JournalEntryDto e : entries) {
            sb.append(e.getEntryNumber()).append(',')
              .append(e.getEntryDate()).append(',')
              .append('"').append(e.getDescription().replace("\"", "'")).append('"').append(',')
              .append(e.getStatus()).append(',')
              .append(e.getTotalDebit()).append(',')
              .append(e.getTotalCredit()).append('\n');
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(sb.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}