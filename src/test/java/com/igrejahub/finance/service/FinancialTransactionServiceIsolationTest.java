package com.igrejahub.finance.service;

import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.finance.dto.FinancialFilterDto;
import com.igrejahub.finance.entity.FinancialTransaction;
import com.igrejahub.finance.mapper.FinancialTransactionMapper;
import com.igrejahub.finance.repository.FinancialCategoryRepository;
import com.igrejahub.finance.repository.FinancialTransactionRepository;
import com.igrejahub.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Regressão: um usuário vinculado a uma congregação não pode, via parâmetro de
 * filtro, sobrescrever a própria congregação e enxergar transações de outra
 * congregação (mesmo dentro da mesma Igreja).
 */
@ExtendWith(MockitoExtension.class)
class FinancialTransactionServiceIsolationTest {

    @Mock private FinancialTransactionRepository transactionRepository;
    @Mock private FinancialCategoryRepository categoryRepository;
    @Mock private FinancialAccountService accountService;
    @Mock private FinancialTransactionMapper transactionMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private JdbcTemplate jdbcTemplate;

    private final Pageable pageable = PageRequest.of(0, 20);

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private FinancialTransactionService newService() {
        return new FinancialTransactionService(
            transactionRepository, categoryRepository, accountService,
            transactionMapper, securityUtils, jdbcTemplate);
    }

    private Long capturedCongregationId() {
        ArgumentCaptor<Long> congCaptor = ArgumentCaptor.forClass(Long.class);
        org.mockito.Mockito.verify(transactionRepository).findByFilters(
            any(), any(), any(), congCaptor.capture(), any(), any(), any());
        return congCaptor.getValue();
    }

    @Test
    void congregationScopedUserCannotOverrideOwnCongregation() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentChurchId(10L);
        TenantContext.setCurrentCongregationId(100L); // pastor da congregação A1
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(securityUtils.isRoot()).thenReturn(false);
        when(transactionRepository.findByFilters(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(Page.empty(pageable));

        FinancialFilterDto filter = FinancialFilterDto.builder().congregationId(200L).build();
        newService().getTransactions(pageable, filter);

        assertEquals(100L, capturedCongregationId(),
            "A congregação efetiva não pode ser sobrescrita por um usuário de congregação");
    }

    @Test
    void churchAdminCanDrillIntoOwnChurchCongregation() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentChurchId(10L);
        TenantContext.setCurrentCongregationId(null); // admin da Igreja, sem congregação fixa
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(securityUtils.isRoot()).thenReturn(false);
        when(transactionRepository.findByFilters(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(Page.empty(pageable));

        FinancialFilterDto filter = FinancialFilterDto.builder().congregationId(101L).build();
        newService().getTransactions(pageable, filter);

        assertEquals(101L, capturedCongregationId(),
            "Admin de Igreja pode restringir a listagem a uma congregação da própria Igreja");
    }

    @Test
    void nonRootCannotOverrideChurchIdFilter() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentChurchId(10L);
        TenantContext.setCurrentCongregationId(null);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(securityUtils.isRoot()).thenReturn(false);
        when(transactionRepository.findByFilters(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(Page.empty(pageable));

        FinancialFilterDto filter = FinancialFilterDto.builder().churchId(999L).build();
        newService().getTransactions(pageable, filter);

        ArgumentCaptor<Long> churchCaptor = ArgumentCaptor.forClass(Long.class);
        org.mockito.Mockito.verify(transactionRepository).findByFilters(
            any(), any(), churchCaptor.capture(), any(), any(), any(), any());
        assertEquals(10L, churchCaptor.getValue(),
            "Usuário não-ROOT não pode sobrescrever a própria Igreja via filtro");
    }

    @Test
    void rootCanFilterByAnyCongregation() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentChurchId(null);
        TenantContext.setCurrentCongregationId(null);
        TenantContext.setRootGlobalMode(true);
        when(securityUtils.isRoot()).thenReturn(true);
        when(securityUtils.getEffectiveChurchId()).thenReturn(null);
        when(transactionRepository.findByFilters(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(Page.empty(pageable));

        FinancialFilterDto filter = FinancialFilterDto.builder()
            .churchId(20L).congregationId(201L).build();
        newService().getTransactions(pageable, filter);

        ArgumentCaptor<Long> churchCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> congCaptor = ArgumentCaptor.forClass(Long.class);
        org.mockito.Mockito.verify(transactionRepository).findByFilters(
            any(), any(), churchCaptor.capture(), congCaptor.capture(), any(), any(), any());
        assertEquals(20L, churchCaptor.getValue());
        assertEquals(201L, congCaptor.getValue());
    }
}
