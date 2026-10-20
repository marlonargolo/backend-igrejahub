package com.igrejahub.congregations.service;

import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.congregations.dto.CongregationDto;
import com.igrejahub.congregations.entity.Congregation;
import com.igrejahub.congregations.mapper.CongregationMapper;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.members.repository.MemberRepository;
import com.igrejahub.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @Mock private MemberRepository memberRepository;

    private final Pageable pageable = PageRequest.of(0, 20);

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private CongregationService newService() {
        return new CongregationService(congregationRepository, churchRepository, congregationMapper, securityUtils, memberRepository);
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

    @Test
    void getCongregations_populatesMemberCount_fromActiveMembersOnly() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(null);
        Congregation congregation = Congregation.builder().churchId(10L).name("Paranaguá").build();
        congregation.setId(50L);
        congregation.setOrganizationId(1L);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(congregationRepository.findByOrganizationIdAndChurchId(1L, 10L, pageable))
            .thenReturn(new PageImpl<>(List.of(congregation), pageable, 1));
        when(congregationMapper.toDto(congregation)).thenReturn(CongregationDto.builder().id(50L).build());
        when(memberRepository.countByFilter(1L, "ATIVO", null, 50L)).thenReturn(7L);

        Page<CongregationDto> result = newService().getCongregations(pageable, null, null);

        assertEquals(7L, result.getContent().get(0).getMemberCount());
    }
}
