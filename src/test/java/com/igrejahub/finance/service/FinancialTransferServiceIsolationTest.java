package com.igrejahub.finance.service;

import com.igrejahub.churches.entity.Church;
import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.finance.entity.FinancialAccount;
import com.igrejahub.finance.entity.FinancialTransfer;
import com.igrejahub.finance.mapper.FinancialTransferMapper;
import com.igrejahub.finance.repository.FinancialTransferRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialTransferServiceIsolationTest {

    @Mock private FinancialTransferRepository transferRepository;
    @Mock private FinancialAccountService accountService;
    @Mock private FinancialTransferMapper transferMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private ChurchRepository churchRepository;

    private final Pageable pageable = PageRequest.of(0, 20);

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private FinancialTransferService newService() {
        return new FinancialTransferService(transferRepository, accountService, transferMapper, securityUtils, churchRepository);
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
    void createTransfer_betweenDifferentChurches_nonRoot_isRejected() {
        FinancialAccount from = FinancialAccount.builder().churchId(10L).build();
        from.setId(1L);
        FinancialAccount to = FinancialAccount.builder().churchId(20L).build();
        to.setId(2L);
        when(accountService.getOwnedAccount(1L)).thenReturn(from);
        when(accountService.getOwnedAccount(2L)).thenReturn(to);

        FinancialTransferService service = newService();
        assertThrows(BusinessException.class, () -> service.createTransfer(
            1L, 2L, BigDecimal.TEN, LocalDate.now(), "teste"));
        verify(transferRepository, never()).save(any());
    }

    @Test
    void createTransfer_betweenDifferentChurches_root_isAlsoRejected() {
        // A capacidade administrativa de ROOT enxergar várias Igrejas não pode
        // transformar esta tela numa transferência entre instituições
        // independentes — a restrição à mesma Igreja vale para todo mundo.
        FinancialAccount from = FinancialAccount.builder().churchId(10L).build();
        from.setId(1L);
        FinancialAccount to = FinancialAccount.builder().churchId(20L).build();
        to.setId(2L);
        when(accountService.getOwnedAccount(1L)).thenReturn(from);
        when(accountService.getOwnedAccount(2L)).thenReturn(to);

        FinancialTransferService service = newService();
        assertThrows(BusinessException.class, () -> service.createTransfer(
            1L, 2L, BigDecimal.TEN, LocalDate.now(), "teste"));
        verify(transferRepository, never()).save(any());
    }

    @Test
    void createTransfer_sedeToCongregationOfSameChurch_isAllowed() {
        // Mesma Igreja (mesmo churchId), sede (congregationId null) → Congregação
        // (congregationId preenchido): deve funcionar normalmente.
        FinancialAccount from = FinancialAccount.builder().churchId(10L).congregationId(null).build();
        from.setId(1L);
        FinancialAccount to = FinancialAccount.builder().churchId(10L).congregationId(200L).build();
        to.setId(2L);
        when(accountService.getOwnedAccount(1L)).thenReturn(from);
        when(accountService.getOwnedAccount(2L)).thenReturn(to);
        when(churchRepository.findById(10L)).thenReturn(Optional.of(Church.builder().name("Vale da Bênção").build()));
        when(transferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newService().createTransfer(1L, 2L, BigDecimal.TEN, LocalDate.now(), "teste");

        ArgumentCaptor<FinancialTransfer> captor = ArgumentCaptor.forClass(FinancialTransfer.class);
        verify(transferRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getChurchId());
        assertEquals(10L, captor.getValue().getToChurchId());
    }
}
