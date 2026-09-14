package com.igrejahub.finance.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.finance.dto.CancelTransactionRequest;
import com.igrejahub.finance.dto.CreateTransactionRequest;
import com.igrejahub.finance.entity.FinancialAccount;
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
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Regressão: força de churchId/congregationId na criação de transações e
 * validação de escopo em update/confirm/cancel (getOwnedTransaction).
 */
@ExtendWith(MockitoExtension.class)
class FinancialTransactionServiceCrudIsolationTest {

    @Mock private FinancialTransactionRepository transactionRepository;
    @Mock private FinancialCategoryRepository categoryRepository;
    @Mock private FinancialAccountService accountService;
    @Mock private FinancialTransactionMapper transactionMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private com.igrejahub.audit.service.AuditLogService auditLogService;

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private FinancialTransactionService newService() {
        return new FinancialTransactionService(
            transactionRepository, categoryRepository, accountService,
            transactionMapper, securityUtils, jdbcTemplate, auditLogService);
    }

    private CreateTransactionRequest baseRequest(Long churchId, Long congId) {
        return CreateTransactionRequest.builder()
            .churchId(churchId)
            .congregationId(congId)
            .accountId(1L)
            .type("REVENUE")
            .description("Dízimo")
            .amount(BigDecimal.TEN)
            .transactionDate(LocalDate.now())
            .build();
    }

    private FinancialTransaction captureSavedTransaction() {
        ArgumentCaptor<FinancialTransaction> captor = ArgumentCaptor.forClass(FinancialTransaction.class);
        org.mockito.Mockito.verify(transactionRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void createTransaction_nonRoot_ignoresChurchIdFromBody() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(null);
        FinancialAccount account = FinancialAccount.builder().churchId(10L).build();
        account.setId(1L);
        when(accountService.getOwnedAccount(1L)).thenReturn(account);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newService().createTransaction(baseRequest(999L, 888L)); // tentativa de forjar outra igreja/congregação

        FinancialTransaction saved = captureSavedTransaction();
        assertEquals(10L, saved.getChurchId());
        assertEquals(null, saved.getCongregationId());
    }

    @Test
    void createTransaction_rootFiltered_forcesSelectedChurch() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentChurchId(10L);
        TenantContext.setCurrentCongregationId(null);
        FinancialAccount account = FinancialAccount.builder().churchId(10L).build();
        account.setId(1L);
        when(accountService.getOwnedAccount(1L)).thenReturn(account);
        when(securityUtils.canViewAll()).thenReturn(false); // ROOT filtrado = não pode "ver tudo"
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newService().createTransaction(baseRequest(999L, null));

        assertEquals(10L, captureSavedTransaction().getChurchId());
    }

    @Test
    void createTransaction_rootGlobal_acceptsChurchIdFromBody() {
        TenantContext.setCurrentTenant(1L);
        FinancialAccount account = FinancialAccount.builder().churchId(20L).build();
        account.setId(1L);
        when(accountService.getOwnedAccount(1L)).thenReturn(account);
        when(securityUtils.canViewAll()).thenReturn(true);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newService().createTransaction(baseRequest(20L, 200L));

        FinancialTransaction saved = captureSavedTransaction();
        assertEquals(20L, saved.getChurchId());
        assertEquals(200L, saved.getCongregationId());
    }

    @Test
    void updateTransaction_outsideOwnChurch_isDenied() {
        TenantContext.setCurrentTenant(1L);
        FinancialTransaction other = FinancialTransaction.builder()
            .churchId(20L)
            .status(FinancialTransaction.TransactionStatus.PENDING)
            .build();
        other.setOrganizationId(1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(other));
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);

        FinancialTransactionService service = newService();
        assertThrows(BusinessException.class,
            () -> service.updateTransaction(1L, new com.igrejahub.finance.dto.UpdateTransactionRequest()));
    }

    @Test
    void cancelTransaction_outsideOwnCongregation_isDenied() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(100L);
        FinancialTransaction other = FinancialTransaction.builder()
            .churchId(10L)
            .congregationId(200L) // outra congregação da mesma Igreja
            .status(FinancialTransaction.TransactionStatus.CONFIRMED)
            .build();
        other.setOrganizationId(1L);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(other));
        when(securityUtils.canViewAll()).thenReturn(false);
        lenient().when(securityUtils.getEffectiveChurchId()).thenReturn(10L);

        FinancialTransactionService service = newService();
        assertThrows(BusinessException.class,
            () -> service.cancelTransaction(1L, new CancelTransactionRequest()));
    }
}
