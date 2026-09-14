package com.igrejahub.accounting.service;

import com.igrejahub.accounting.entity.JournalEntry;
import com.igrejahub.accounting.mapper.JournalEntryLineMapper;
import com.igrejahub.accounting.mapper.JournalEntryMapper;
import com.igrejahub.accounting.repository.AccountingPeriodRepository;
import com.igrejahub.accounting.repository.ChartOfAccountRepository;
import com.igrejahub.accounting.repository.JournalEntryRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Regressão: JournalEntry (lançamentos contábeis) tinha churchId na entidade
 * mas nenhuma query o utilizava — a listagem retornava lançamentos de todas
 * as Igrejas da organização para qualquer usuário com ACCOUNTING_VIEW.
 */
@ExtendWith(MockitoExtension.class)
class JournalEntryServiceIsolationTest {

    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private ChartOfAccountRepository chartOfAccountRepository;
    @Mock private AccountingPeriodRepository periodRepository;
    @Mock private JournalEntryMapper journalEntryMapper;
    @Mock private JournalEntryLineMapper lineMapper;
    @Mock private SecurityUtils securityUtils;

    private final Pageable pageable = PageRequest.of(0, 20);

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private JournalEntryService newService() {
        return new JournalEntryService(journalEntryRepository, chartOfAccountRepository,
            periodRepository, journalEntryMapper, lineMapper, securityUtils);
    }

    @Test
    void getEntries_churchScopedUser_onlySeesOwnChurchEntries() {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(journalEntryRepository.findByOrganizationIdAndChurchId(1L, 10L, pageable))
            .thenReturn(Page.empty(pageable));

        newService().getEntries(pageable, null);

        verify(journalEntryRepository).findByOrganizationIdAndChurchId(1L, 10L, pageable);
        verify(journalEntryRepository, never()).findByOrganizationId(anyLong(), any());
    }

    @Test
    void getEntries_rootGlobal_seesAllChurches() {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.canViewAll()).thenReturn(true);
        when(journalEntryRepository.findByOrganizationId(1L, pageable)).thenReturn(Page.empty(pageable));

        newService().getEntries(pageable, null);

        verify(journalEntryRepository).findByOrganizationId(1L, pageable);
    }

    @Test
    void getEntry_crossChurchAccess_isDenied() {
        TenantContext.setCurrentTenant(1L);
        JournalEntry other = new JournalEntry();
        other.setOrganizationId(1L);
        other.setChurchId(20L);
        when(journalEntryRepository.findByOrganizationIdAndId(1L, 5L)).thenReturn(Optional.of(other));
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);

        JournalEntryService service = newService();
        assertThrows(BusinessException.class, () -> service.getEntry(5L));
    }
}
