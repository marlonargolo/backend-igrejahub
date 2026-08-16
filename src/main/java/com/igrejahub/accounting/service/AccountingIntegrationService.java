package com.igrejahub.accounting.service;

import com.igrejahub.accounting.dto.CreateJournalEntryRequest;
import com.igrejahub.accounting.dto.JournalEntryDto;
import com.igrejahub.accounting.dto.JournalEntryLineDto;
import com.igrejahub.accounting.repository.ChartOfAccountRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountingIntegrationService {

    private final JournalEntryService journalEntryService;
    private final ChartOfAccountRepository chartOfAccountRepository;

    /**
     * Gera automaticamente o lançamento contábil em partida dobrada quando uma
     * transação financeira é confirmada. Ex.: receita de dízimo confirmada ->
     * debita "Caixa/Banco" e credita a conta de receita mapeada pela categoria.
     * Retorna null (sem lançar exceção) se não houver plano de contas configurado,
     * para não travar o fluxo financeiro por falta de setup contábil.
     */
    @Transactional
    public JournalEntryDto generateEntryForTransaction(Long cashAccountChartId, Long counterpartChartAccountId,
                                                         long amountCents, boolean isRevenue,
                                                         LocalDate date, String description, String reference) {
        Long orgId = TenantContext.getCurrentTenant();
        if (cashAccountChartId == null || counterpartChartAccountId == null) {
            log.warn("Integração contábil pulada: contas do plano de contas não mapeadas para org {}", orgId);
            return null;
        }
        if (chartOfAccountRepository.findByOrganizationIdAndId(orgId, cashAccountChartId).isEmpty()) {
            throw new BusinessException("Conta de caixa/banco não encontrada no plano de contas");
        }
        if (chartOfAccountRepository.findByOrganizationIdAndId(orgId, counterpartChartAccountId).isEmpty()) {
            throw new BusinessException("Conta contrapartida não encontrada no plano de contas");
        }

        JournalEntryLineDto cashLine = new JournalEntryLineDto();
        cashLine.setAccountId(cashAccountChartId);
        cashLine.setDebitCents(isRevenue ? amountCents : 0L);
        cashLine.setCreditCents(isRevenue ? 0L : amountCents);
        cashLine.setDescription(description);

        JournalEntryLineDto counterpartLine = new JournalEntryLineDto();
        counterpartLine.setAccountId(counterpartChartAccountId);
        counterpartLine.setDebitCents(isRevenue ? 0L : amountCents);
        counterpartLine.setCreditCents(isRevenue ? amountCents : 0L);
        counterpartLine.setDescription(description);

        CreateJournalEntryRequest request = new CreateJournalEntryRequest();
        request.setEntryDate(date);
        request.setDescription(description);
        request.setReference(reference);
        request.setLines(List.of(cashLine, counterpartLine));

        return journalEntryService.createEntry(request);
    }
}