package com.igrejahub.members.service;

import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.members.repository.MemberRepository;
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
 * ROOT precisa respeitar o mesmo isolamento de Congregação que qualquer outro
 * usuário quando "entra" numa Congregação específica — dados da Igreja não
 * podem aparecer dentro da Congregação, nem mesmo para ROOT.
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceIsolationTest {

    @Mock private MemberRepository memberRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private CongregationRepository congregationRepository;
    @Mock private SecurityUtils securityUtils;

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private MemberService newService() {
        return new MemberService(memberRepository, churchRepository, congregationRepository, securityUtils);
    }

    @Test
    void getMembers_rootInsideChurchAndCongregation_filtersByCongregationOnly() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(100L);
        Pageable pageable = PageRequest.of(0, 20);

        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(memberRepository.findByCongregationId(eq(1L), eq(100L), any(), any(), eq(pageable)))
            .thenReturn(Page.empty(pageable));

        newService().getMembers(pageable, null, null, null, null);

        verify(memberRepository).findByCongregationId(eq(1L), eq(100L), any(), any(), eq(pageable));
        verify(memberRepository, never()).findByChurchId(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getMembers_rootInsideChurchOnly_stillSeesWholeChurch() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(null);
        Pageable pageable = PageRequest.of(0, 20);

        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(memberRepository.findByChurchId(eq(1L), eq(10L), any(), any(), any(), eq(pageable)))
            .thenReturn(Page.empty(pageable));

        newService().getMembers(pageable, null, null, null, null);

        verify(memberRepository).findByChurchId(eq(1L), eq(10L), any(), any(), any(), eq(pageable));
        verify(memberRepository, never()).findByCongregationId(any(), any(), any(), any(), any());
    }
}
