package com.igrejahub.finance.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.finance.entity.FinancialAccount;
import com.igrejahub.finance.mapper.FinancialTransferMapper;
import com.igrejahub.finance.repository.FinancialTransferRepository;
import com.igrejahub.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialTransferServiceIsolationTest {

    @Mock private FinancialTransferRepository transferRepository;
    @Mock private FinancialAccountService accountService;
    @Mock private FinancialTransferMapper transferMapper;
    @Mock private SecurityUtils securityUtils;

    private final Pageable pageable = PageRequest.of(0, 20);

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private FinancialTransferService newService() {
        return new FinancialTransferService(transferRepository, accountService, transferMapper, securityUtils);
    }

    @Test
    void getTransfers_churchScopedUser_onlySeesOwnChurchTransfers() {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(transferRepository.findByOrganizationIdAndChurchId(1L, 10L, pageable))
            .thenReturn(Page.empty(pageable));

        newService().getTransfers(pageable);

        verify(transferRepository).findByOrganizationIdAndChurchId(1L, 10L, pageable);
        verify(transferRepository, never()).findByOrganizationId(anyLong(), any());
    }

    @Test
    void getTransfers_rootGlobal_seesAllChurches() {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.canViewAll()).thenReturn(true);
        when(transferRepository.findByOrganizationId(1L, pageable)).thenReturn(Page.empty(pageable));

        newService().getTransfers(pageable);

        verify(transferRepository).findByOrganizationId(1L, pageable);
    }

    @Test
    void createTransfer_betweenDifferentChurches_isRejected() {
        FinancialAccount from = FinancialAccount.builder().churchId(10L).build();
        from.setId(1L);
        FinancialAccount to = FinancialAccount.builder().churchId(20L).build();
        to.setId(2L);
        when(accountService.getOwnedAccount(1L)).thenReturn(from);
        when(accountService.getOwnedAccount(2L)).thenReturn(to);

        FinancialTransferService service = newService();
        assertThrows(BusinessException.class, () -> service.createTransfer(
            1L, 2L, BigDecimal.TEN, LocalDate.now(), "teste"));
    }
}
