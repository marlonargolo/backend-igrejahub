package com.igrejahub.finance.service;

import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.finance.entity.FinancialAccount;
import com.igrejahub.finance.mapper.FinancialAccountMapper;
import com.igrejahub.finance.repository.FinancialAccountRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialAccountServiceIsolationTest {

    @Mock private FinancialAccountRepository accountRepository;
    @Mock private FinancialAccountMapper accountMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private ChurchRepository churchRepository;
    @Mock private CongregationRepository congregationRepository;

    private final Pageable pageable = PageRequest.of(0, 20);

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private FinancialAccountService newService() {
        return new FinancialAccountService(accountRepository, accountMapper, securityUtils, churchRepository, congregationRepository);
    }

    @Test
    void getAccounts_churchScopedUser_onlySeesOwnChurchAccounts() {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(accountRepository.findByOrganizationIdAndChurchId(1L, 10L, pageable))
            .thenReturn(Page.empty(pageable));

        newService().getAccounts(pageable);

        verify(accountRepository).findByOrganizationIdAndChurchId(1L, 10L, pageable);
        verify(accountRepository, never()).findByOrganizationId(any(), any());
    }

    @Test
    void getAccounts_rootGlobal_seesAllChurches() {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.canViewAll()).thenReturn(true);
        when(accountRepository.findByOrganizationId(1L, pageable)).thenReturn(Page.empty(pageable));

        newService().getAccounts(pageable);

        verify(accountRepository).findByOrganizationId(1L, pageable);
        verify(accountRepository, never()).findByOrganizationIdAndChurchId(any(), any(), any());
    }

    @Test
    void getAccounts_userWithoutChurch_returnsEmpty() {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(null);

        Page<?> result = newService().getAccounts(pageable);

        assertTrue(result.isEmpty());
        verifyNoInteractions(accountRepository);
    }

    @Test
    void getAccount_crossChurchAccess_isDenied() {
        TenantContext.setCurrentTenant(1L);
        FinancialAccount otherChurchAccount = FinancialAccount.builder().churchId(20L).build();
        otherChurchAccount.setOrganizationId(1L);
        when(accountRepository.findById(5L)).thenReturn(Optional.of(otherChurchAccount));
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);

        FinancialAccountService service = newService();
        assertThrows(BusinessException.class, () -> service.getAccount(5L));
    }

    @Test
    void createAccount_nonRoot_forcesOwnChurchId() {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newService().createAccount("Caixa", "CASH", null, null, null,
            java.math.BigDecimal.ZERO, 999L /* tentativa de forjar outra igreja */);

        org.mockito.ArgumentCaptor<FinancialAccount> captor =
            org.mockito.ArgumentCaptor.forClass(FinancialAccount.class);
        verify(accountRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getChurchId());
        assertNull(captor.getValue().getCongregationId());
    }

    @Test
    void createAccount_enteredInCongregation_accountBelongsToThatCongregation() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(200L);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newService().createAccount("Caixa Paranaguá", "CASH", null, null, null,
            java.math.BigDecimal.ZERO, null);

        org.mockito.ArgumentCaptor<FinancialAccount> captor =
            org.mockito.ArgumentCaptor.forClass(FinancialAccount.class);
        verify(accountRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getChurchId());
        assertEquals(200L, captor.getValue().getCongregationId());
    }

    @Test
    void getActiveAccountsForUnit_noCongregation_returnsSedeLocalAccounts() {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(accountRepository.findByOrganizationIdAndChurchIdAndCongregationIdIsNullAndActiveTrue(1L, 10L))
            .thenReturn(java.util.List.of());

        newService().getActiveAccountsForUnit(null);

        verify(accountRepository).findByOrganizationIdAndChurchIdAndCongregationIdIsNullAndActiveTrue(1L, 10L);
        verify(accountRepository, never()).findByOrganizationIdAndCongregationIdAndActiveTrue(any(), any());
    }

    @Test
    void getActiveAccountsForUnit_validCongregationOfCurrentChurch_returnsItsAccounts() {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(congregationRepository.existsByOrganizationIdAndIdAndChurchId(1L, 200L, 10L)).thenReturn(true);
        when(accountRepository.findByOrganizationIdAndCongregationIdAndActiveTrue(1L, 200L))
            .thenReturn(java.util.List.of());

        newService().getActiveAccountsForUnit(200L);

        verify(accountRepository).findByOrganizationIdAndCongregationIdAndActiveTrue(1L, 200L);
    }

    @Test
    void getActiveAccountsForUnit_congregationFromAnotherChurch_isRejected() {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(congregationRepository.existsByOrganizationIdAndIdAndChurchId(1L, 999L, 10L)).thenReturn(false);

        FinancialAccountService service = newService();
        assertThrows(BusinessException.class, () -> service.getActiveAccountsForUnit(999L));
    }
}
