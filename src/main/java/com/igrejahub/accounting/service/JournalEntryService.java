package com.igrejahub.accounting.service;

import com.igrejahub.accounting.dto.CreateJournalEntryRequest;
import com.igrejahub.accounting.dto.JournalEntryDto;
import com.igrejahub.accounting.dto.JournalEntryLineDto;
import com.igrejahub.accounting.dto.UpdateJournalEntryRequest;
import com.igrejahub.accounting.entity.ChartOfAccount;
import com.igrejahub.accounting.entity.JournalEntry;
import com.igrejahub.accounting.entity.JournalEntryLine;
import com.igrejahub.accounting.mapper.JournalEntryLineMapper;
import com.igrejahub.accounting.mapper.JournalEntryMapper;
import com.igrejahub.accounting.repository.AccountingPeriodRepository;
import com.igrejahub.accounting.repository.ChartOfAccountRepository;
import com.igrejahub.accounting.repository.JournalEntryRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final AccountingPeriodRepository periodRepository;
    private final JournalEntryMapper journalEntryMapper;
    private final JournalEntryLineMapper lineMapper;

    public Page<JournalEntryDto> getEntries(Pageable pageable, String status) {
        Long orgId = TenantContext.getCurrentTenant();
        Page<JournalEntry> page = status != null
                ? journalEntryRepository.findByOrganizationIdAndStatus(orgId, status, pageable)
                : journalEntryRepository.findByOrganizationId(orgId, pageable);
        return page.map(this::toDtoWithTotals);
    }

    public JournalEntryDto getEntry(Long id) {
        return toDtoWithTotals(getOwned(id));
    }

    @Transactional
    public JournalEntryDto createEntry(CreateJournalEntryRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        validateBalanced(request.getLines());
        validateAccounts(orgId, request.getLines());

        // O período contábil precisa estar aberto para a data do lançamento
        periodRepository.findByOrganizationIdAndDate(orgId, request.getEntryDate())
                .filter(p -> "OPEN".equals(p.getStatus()))
                .orElseThrow(() -> new BusinessException(
                        "Não há período contábil aberto para a data " + request.getEntryDate()));

        JournalEntry entry = new JournalEntry();
        entry.setOrganizationId(orgId);
        entry.setChurchId(request.getChurchId());
        entry.setEntryNumber(generateEntryNumber(orgId, request.getEntryDate()));
        entry.setEntryDate(request.getEntryDate());
        entry.setDescription(request.getDescription());
        entry.setReference(request.getReference());
        entry.setStatus("DRAFT");

        List<JournalEntryLine> lines = new ArrayList<>();
        for (JournalEntryLineDto lineDto : request.getLines()) {
            JournalEntryLine line = new JournalEntryLine();
            line.setJournalEntry(entry);
            line.setAccountId(lineDto.getAccountId());
            line.setDebitCents(lineDto.getDebitCents());
            line.setCreditCents(lineDto.getCreditCents());
            line.setDescription(lineDto.getDescription());
            lines.add(line);
        }
        entry.setLines(lines);

        entry = journalEntryRepository.save(entry);
        return toDtoWithTotals(entry);
    }

    @Transactional
    public JournalEntryDto updateEntry(Long id, UpdateJournalEntryRequest request) {
        JournalEntry entry = getOwned(id);
        if (!"DRAFT".equals(entry.getStatus())) {
            throw new BusinessException("Somente lançamentos em rascunho podem ser editados");
        }
        if (request.getEntryDate() != null) entry.setEntryDate(request.getEntryDate());
        if (request.getDescription() != null) entry.setDescription(request.getDescription());
        if (request.getReference() != null) entry.setReference(request.getReference());

        if (request.getLines() != null && !request.getLines().isEmpty()) {
            validateBalanced(request.getLines());
            validateAccounts(entry.getOrganizationId(), request.getLines());
            entry.getLines().clear();
            for (JournalEntryLineDto lineDto : request.getLines()) {
                JournalEntryLine line = new JournalEntryLine();
                line.setJournalEntry(entry);
                line.setAccountId(lineDto.getAccountId());
                line.setDebitCents(lineDto.getDebitCents());
                line.setCreditCents(lineDto.getCreditCents());
                line.setDescription(lineDto.getDescription());
                entry.getLines().add(line);
            }
        }
        return toDtoWithTotals(journalEntryRepository.save(entry));
    }

    /** Contabiliza (POST) o lançamento — depois disso ele se torna imutável. */
    @Transactional
    public JournalEntryDto postEntry(Long id) {
        JournalEntry entry = getOwned(id);
        if (!"DRAFT".equals(entry.getStatus())) {
            throw new BusinessException("Somente lançamentos em rascunho podem ser contabilizados");
        }
        validateBalanced(toLineDtos(entry.getLines()));
        entry.setStatus("POSTED");
        return toDtoWithTotals(journalEntryRepository.save(entry));
    }

    @Transactional
    public JournalEntryDto cancelEntry(Long id) {
        JournalEntry entry = getOwned(id);
        if ("CANCELLED".equals(entry.getStatus())) {
            throw new BusinessException("Lançamento já está cancelado");
        }
        entry.setStatus("CANCELLED");
        return toDtoWithTotals(journalEntryRepository.save(entry));
    }

    private void validateBalanced(List<JournalEntryLineDto> lines) {
        if (lines == null || lines.size() < 2) {
            throw new BusinessException("Um lançamento contábil precisa de ao menos 2 linhas");
        }
        long totalDebit = lines.stream().mapToLong(l -> l.getDebitCents() != null ? l.getDebitCents() : 0L).sum();
        long totalCredit = lines.stream().mapToLong(l -> l.getCreditCents() != null ? l.getCreditCents() : 0L).sum();
        if (totalDebit != totalCredit) {
            throw new BusinessException(String.format(
                    "Lançamento não está balanceado: débitos (%d) ≠ créditos (%d)", totalDebit, totalCredit));
        }
        if (totalDebit == 0) {
            throw new BusinessException("Lançamento não pode ter valor zero");
        }
        for (JournalEntryLineDto line : lines) {
            long debit = line.getDebitCents() != null ? line.getDebitCents() : 0L;
            long credit = line.getCreditCents() != null ? line.getCreditCents() : 0L;
            if (debit > 0 && credit > 0) {
                throw new BusinessException("Uma linha não pode ter débito e crédito ao mesmo tempo");
            }
            if (debit == 0 && credit == 0) {
                throw new BusinessException("Toda linha precisa ter um valor de débito ou crédito");
            }
        }
    }

    private void validateAccounts(Long orgId, List<JournalEntryLineDto> lines) {
        for (JournalEntryLineDto line : lines) {
            ChartOfAccount account = chartOfAccountRepository.findByOrganizationIdAndId(orgId, line.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("ChartOfAccount", line.getAccountId()));
            if (!account.getAnalytical()) {
                throw new BusinessException("A conta '" + account.getName() + "' é sintética e não aceita lançamentos diretos");
            }
            if (!account.getActive()) {
                throw new BusinessException("A conta '" + account.getName() + "' está inativa");
            }
        }
    }

    private String generateEntryNumber(Long orgId, LocalDate date) {
        String prefix = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
        long sequence = journalEntryRepository.countByOrganizationId(orgId) + 1;
        String candidate = String.format("%s-%05d", prefix, sequence);
        while (journalEntryRepository.existsByOrganizationIdAndEntryNumber(orgId, candidate)) {
            sequence++;
            candidate = String.format("%s-%05d", prefix, sequence);
        }
        return candidate;
    }

    private List<JournalEntryLineDto> toLineDtos(List<JournalEntryLine> lines) {
        return lines.stream().map(lineMapper::toDto).toList();
    }

    private JournalEntryDto toDtoWithTotals(JournalEntry entry) {
        JournalEntryDto dto = journalEntryMapper.toDto(entry);
        List<JournalEntryLineDto> lines = toLineDtos(entry.getLines());
        dto.setLines(lines);
        dto.setTotalDebit(lines.stream().mapToLong(l -> l.getDebitCents() != null ? l.getDebitCents() : 0L).sum());
        dto.setTotalCredit(lines.stream().mapToLong(l -> l.getCreditCents() != null ? l.getCreditCents() : 0L).sum());
        return dto;
    }

    private JournalEntry getOwned(Long id) {
        return journalEntryRepository.findByOrganizationIdAndId(TenantContext.getCurrentTenant(), id)
                .orElseThrow(() -> new ResourceNotFoundException("JournalEntry", id));
    }
}