package com.igrejahub.dashboard.service;

import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.dashboard.dto.DashboardFilterDto;
import com.igrejahub.finance.repository.FinancialTransactionRepository;
import com.igrejahub.members.repository.MemberRepository;
import com.igrejahub.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regressão: o dashboard confiava cegamente em churchId/congregationId
 * enviados pelo cliente na query string, sem checar o usuário autenticado.
 * Um usuário de congregação (ou até qualquer usuário) podia ver métricas de
 * qualquer Igreja/Congregação da organização — e, na falta do frontend
 * enviar congregationId (que nem existe na UI), um usuário de congregação
 * sempre via os números agregados da Igreja inteira em vez dos da própria
 * congregação, exatamente o sintoma relatado ("cai na igreja principal").
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceIsolationTest {

    @Mock private MemberRepository memberRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private CongregationRepository congregationRepository;
    @Mock private FinancialTransactionRepository transactionRepository;
    @Mock private SecurityUtils securityUtils;

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private DashboardService newService() {
        return new DashboardService(memberRepository, churchRepository, congregationRepository,
            transactionRepository, securityUtils);
    }

    @Test
    void congregationScopedUser_ignoresClientSuppliedChurchAndCongregation() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(100L); // ex.: tesoureiro da congregação A1
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);

        // Tenta forjar outra Igreja/Congregação via parâmetros da requisição
        DashboardFilterDto filter = DashboardFilterDto.builder()
            .churchId(999L).congregationId(888L).build();
        newService().getDashboardMetrics(filter);

        ArgumentCaptor<Long> churchCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> congCaptor = ArgumentCaptor.forClass(Long.class);
        verify(memberRepository).countByFilter(any(), any(), churchCaptor.capture(), congCaptor.capture());
        assertEquals(10L, churchCaptor.getValue(), "churchId deve vir do contexto, nunca do cliente");
        assertEquals(100L, congCaptor.getValue(), "congregationId deve ser sempre a própria congregação");

        verify(transactionRepository).sumConfirmedRevenueCentsByFilter(any(), any(), any(), eq(10L), eq(100L));
    }

    @Test
    void churchAdmin_withoutCongregation_seesWholeChurchByDefault() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(null);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(congregationRepository.countByChurchId(10L)).thenReturn(3L);

        DashboardFilterDto filter = DashboardFilterDto.builder().build(); // sem filtro nenhum
        var metrics = newService().getDashboardMetrics(filter);

        ArgumentCaptor<Long> congCaptor = ArgumentCaptor.forClass(Long.class);
        verify(memberRepository).countByFilter(any(), any(), eq(10L), congCaptor.capture());
        assertNull(congCaptor.getValue(), "sem congregação própria, sem filtro explícito → vê a Igreja toda");
        assertEquals(1L, metrics.getTotalChurches());
        assertEquals(3L, metrics.getTotalCongregations());
    }

    @Test
    void churchAdmin_cannotDrillIntoCongregationFromAnotherChurch() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(null);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(congregationRepository.existsByOrganizationIdAndIdAndChurchId(1L, 500L, 10L)).thenReturn(false);

        DashboardFilterDto filter = DashboardFilterDto.builder().congregationId(500L).build();
        newService().getDashboardMetrics(filter);

        ArgumentCaptor<Long> congCaptor = ArgumentCaptor.forClass(Long.class);
        verify(memberRepository).countByFilter(any(), any(), eq(10L), congCaptor.capture());
        assertNull(congCaptor.getValue(), "congregação de outra Igreja nunca é aplicada");
    }

    @Test
    void rootGlobal_canFilterFreely() {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.canViewAll()).thenReturn(true);
        when(churchRepository.countByOrganizationIdAndStatus(1L, "ACTIVE")).thenReturn(5L);
        when(congregationRepository.countByOrganizationIdAndStatus(1L, "ACTIVE")).thenReturn(20L);

        DashboardFilterDto filter = DashboardFilterDto.builder().churchId(20L).congregationId(200L).build();
        newService().getDashboardMetrics(filter);

        verify(memberRepository).countByFilter(any(), any(), eq(20L), eq(200L));
    }
}
