package com.igrejahub.members.service;

import com.igrejahub.audit.service.AuditLogService;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.members.entity.Member;
import com.igrejahub.members.repository.MemberRepository;
import com.igrejahub.members.repository.MemberTransferRepository;
import com.igrejahub.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Transferência real entre Congregações da mesma Igreja — a regra que proíbe
 * transferir para uma Congregação de outra Igreja é a parte crítica a provar.
 */
@ExtendWith(MockitoExtension.class)
class MemberTransferServiceTest {

    @Mock private MemberTransferRepository transferRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private CongregationRepository congregationRepository;
    @Mock private MemberService memberService;
    @Mock private SecurityUtils securityUtils;
    @Mock private AuditLogService auditLogService;

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private MemberTransferService newService() {
        return new MemberTransferService(transferRepository, memberRepository,
            congregationRepository, memberService, securityUtils, auditLogService);
    }

    @Test
    void transferMember_toCongregationOfAnotherChurch_isRejected() {
        TenantContext.setCurrentTenant(1L);
        Member member = new Member();
        member.setId(5L);
        member.setChurchId(10L);
        member.setCongregationId(100L);
        when(memberRepository.findById(5L)).thenReturn(Optional.of(member));
        when(congregationRepository.existsByOrganizationIdAndIdAndChurchId(1L, 999L, 10L)).thenReturn(false);

        MemberTransferService service = newService();
        assertThrows(BusinessException.class, () -> service.transferMember(5L, 999L, "mudou de bairro"));
        verify(memberRepository, never()).save(any());
        verify(transferRepository, never()).save(any());
    }

    @Test
    void transferMember_toValidCongregation_movesTheMemberAndRecordsTransfer() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentUserId(7L);
        Member member = new Member();
        member.setId(5L);
        member.setChurchId(10L);
        member.setCongregationId(100L);
        member.setName("Fulano");
        when(memberRepository.findById(5L)).thenReturn(Optional.of(member));
        when(congregationRepository.existsByOrganizationIdAndIdAndChurchId(1L, 200L, 10L)).thenReturn(true);
        when(transferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newService().transferMember(5L, 200L, "mudou de bairro");

        assertEquals(200L, member.getCongregationId());
        verify(memberRepository).save(member);
        ArgumentCaptor<com.igrejahub.members.entity.MemberTransfer> captor =
            ArgumentCaptor.forClass(com.igrejahub.members.entity.MemberTransfer.class);
        verify(transferRepository).save(captor.capture());
        assertEquals(100L, captor.getValue().getFromCongregationId());
        assertEquals(200L, captor.getValue().getToCongregationId());
    }

    @Test
    void transferMember_toSameCongregation_isRejected() {
        TenantContext.setCurrentTenant(1L);
        Member member = new Member();
        member.setId(5L);
        member.setChurchId(10L);
        member.setCongregationId(100L);
        when(memberRepository.findById(5L)).thenReturn(Optional.of(member));
        when(congregationRepository.existsByOrganizationIdAndIdAndChurchId(1L, 100L, 10L)).thenReturn(true);

        MemberTransferService service = newService();
        assertThrows(BusinessException.class, () -> service.transferMember(5L, 100L, null));
        verify(transferRepository, never()).save(any());
    }
}
