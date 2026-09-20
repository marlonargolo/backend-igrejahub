package com.igrejahub.congregations.service;

import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.congregations.mapper.CongregationMapper;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regressão: getCongregations(churchIdParam) deixava o parâmetro da
 * requisição sobrescrever a Igreja do usuário para QUALQUER chamador, não só
 * ROOT. Um admin da Igreja A podia passar ?churchId=<Igreja B> e receber as
 * congregações da Igreja B inteira.
 */
@ExtendWith(MockitoExtension.class)
class CongregationServiceIsolationTest {

    @Mock private CongregationRepository congregationRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private CongregationMapper congregationMapper;
    @Mock private SecurityUtils securityUtils;

    private final Pageable pageable = PageRequest.of(0, 20);

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private CongregationService newService() {
        return new CongregationService(congregationRepository, churchRepository, congregationMapper, securityUtils);
    }

    @Test
    void nonRootChurchAdmin_cannotOverrideChurchIdParam() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(null);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(congregationRepository.findByOrganizationIdAndChurchId(1L, 10L, pageable))
            .thenReturn(Page.empty(pageable));

        // Tenta forjar a Igreja B (id 999) via parâmetro de query
        newService().getCongregations(pageable, 999L, null);

        verify(congregationRepository).findByOrganizationIdAndChurchId(1L, 10L, pageable);
        verify(congregationRepository, never()).findByOrganizationIdAndChurchId(eq(1L), eq(999L), any());
    }

    @Test
    void rootGlobal_canFilterByAnyChurch() {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.canViewAll()).thenReturn(true);
        when(congregationRepository.findByOrganizationIdAndChurchId(1L, 999L, pageable))
            .thenReturn(Page.empty(pageable));

        newService().getCongregations(pageable, 999L, null);

        verify(congregationRepository).findByOrganizationIdAndChurchId(1L, 999L, pageable);
    }
}
