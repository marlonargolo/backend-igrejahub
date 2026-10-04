package com.igrejahub.members.service;

import com.igrejahub.audit.repository.AuditLogRepository;
import com.igrejahub.audit.service.AuditLogService;
import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.members.dto.UpdateMemberRequest;
import com.igrejahub.members.entity.Member;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    @Mock private AuditLogService auditLogService;
    @Mock private AuditLogRepository auditLogRepository;

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private MemberService newService() {
        return new MemberService(memberRepository, churchRepository, congregationRepository,
            securityUtils, auditLogService, auditLogRepository);
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

    @Test
    void getMembers_restrictedWithoutResolvedCongregation_returnsEmpty_neverWholeChurch() {
        // Usuário com accessMainChurch=false e mais de uma Congregação vinculada,
        // sem ter escolhido nenhuma ainda (TenantIsolationFilter manda congId=null
        // + mainChurchAccessDenied=true nesse caso) — nunca pode cair no fallback
        // de Igreja inteira, mesmo tendo um churchId efetivo válido.
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(null);
        TenantContext.setMainChurchAccessDenied(true);
        Pageable pageable = PageRequest.of(0, 20);

        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);

        var result = newService().getMembers(pageable, null, null, null, null);

        assertEquals(0, result.getTotalElements());
        verify(memberRepository, never()).findByChurchId(any(), any(), any(), any(), any(), any());
        verify(memberRepository, never()).findByCongregationId(any(), any(), any(), any(), any());
    }

    @Test
    void updateMember_statusChange_logsAuditAction() {
        TenantContext.setCurrentTenant(1L);
        Member member = new Member();
        member.setId(5L);
        member.setOrganizationId(1L);
        member.setStatus("ATIVO");
        when(memberRepository.findById(5L)).thenReturn(Optional.of(member));
        when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateMemberRequest request = new UpdateMemberRequest();
        request.setStatus("INATIVO");
        newService().updateMember(5L, request);

        assertEquals("INATIVO", member.getStatus());
        verify(auditLogService).logAction(eq("UPDATE_MEMBER_STATUS"), eq("MEMBER"), eq(5L), any(), any());
    }

    @Test
    void updateMember_sameStatus_doesNotLogAuditAction() {
        TenantContext.setCurrentTenant(1L);
        Member member = new Member();
        member.setId(5L);
        member.setOrganizationId(1L);
        member.setStatus("ATIVO");
        when(memberRepository.findById(5L)).thenReturn(Optional.of(member));
        when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateMemberRequest request = new UpdateMemberRequest();
        request.setStatus("ATIVO");
        newService().updateMember(5L, request);

        verify(auditLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    @Test
    void deleteMember_setsInativoStatus_andLogsAuditAction() {
        TenantContext.setCurrentTenant(1L);
        Member member = new Member();
        member.setId(5L);
        member.setOrganizationId(1L);
        member.setStatus("ATIVO");
        when(memberRepository.findById(5L)).thenReturn(Optional.of(member));
        when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newService().deleteMember(5L);

        assertEquals("INATIVO", member.getStatus());
        verify(auditLogService, times(1)).logAction(eq("UPDATE_MEMBER_STATUS"), eq("MEMBER"), eq(5L), any(), any());
    }
}
