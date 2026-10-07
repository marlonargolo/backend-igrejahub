package com.igrejahub.members.service;

import com.igrejahub.audit.service.AuditLogService;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.members.dto.MemberTransferDto;
import com.igrejahub.members.entity.Member;
import com.igrejahub.members.entity.MemberTransfer;
import com.igrejahub.members.repository.MemberRepository;
import com.igrejahub.members.repository.MemberTransferRepository;
import com.igrejahub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberTransferService {

    private final MemberTransferRepository transferRepository;
    private final MemberRepository         memberRepository;
    private final CongregationRepository   congregationRepository;
    private final MemberService            memberService;
    private final SecurityUtils            securityUtils;
    private final AuditLogService          auditLogService;

    @Transactional
    public MemberTransferDto transferMember(Long memberId, Long toCongregationId, String reason) {
        memberService.getMember(memberId); // reaproveita isolamento (org/Igreja/Congregação)
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));

        Long orgId = TenantContext.getCurrentTenant();
        if (!congregationRepository.existsByOrganizationIdAndIdAndChurchId(orgId, toCongregationId, member.getChurchId())) {
            throw new BusinessException("A congregação destino não pertence à mesma Igreja do membro.");
        }

        Long fromCongregationId = member.getCongregationId();
        if (toCongregationId.equals(fromCongregationId)) {
            throw new BusinessException("O membro já está nesta congregação.");
        }

        member.setCongregationId(toCongregationId);
        memberRepository.save(member);

        MemberTransfer transfer = MemberTransfer.builder()
            .organizationId(orgId)
            .churchId(member.getChurchId())
            .memberId(memberId)
            .fromCongregationId(fromCongregationId)
            .toCongregationId(toCongregationId)
            .reason(reason)
            .transferredBy(TenantContext.getCurrentUserId())
            .transferredAt(LocalDateTime.now())
            .build();
        transfer = transferRepository.save(transfer);

        log.info("Member {} transferred from congregationId={} to congregationId={} by={}",
            memberId, fromCongregationId, toCongregationId, TenantContext.getCurrentUserId());
        auditLogService.logAction("TRANSFER_MEMBER", "MEMBER", memberId,
            Map.of("congregationId", String.valueOf(fromCongregationId)),
            Map.of("congregationId", String.valueOf(toCongregationId)));

        return toDto(transfer, member.getName());
    }

    public Page<MemberTransferDto> listTransfers(Pageable pageable) {
        Long orgId = TenantContext.getCurrentTenant();
        Page<MemberTransfer> page;

        if (securityUtils.canViewAll()) {
            page = transferRepository.findByOrganizationIdOrderByTransferredAtDesc(orgId, pageable);
        } else {
            Long effCongId = TenantContext.getCurrentCongregationId();
            if (effCongId != null) {
                page = transferRepository.findByOrganizationIdAndCongregation(orgId, effCongId, pageable);
            } else if (TenantContext.isMainChurchAccessDenied()) {
                return Page.empty(pageable);
            } else {
                Long churchId = securityUtils.getEffectiveChurchId();
                if (churchId == null) return Page.empty(pageable);
                page = transferRepository.findByOrganizationIdAndChurchIdOrderByTransferredAtDesc(orgId, churchId, pageable);
            }
        }

        return page.map(this::enrichDto);
    }

    private MemberTransferDto enrichDto(MemberTransfer t) {
        String memberName = memberRepository.findById(t.getMemberId())
            .map(Member::getName).orElse("—");
        String fromName = t.getFromCongregationId() != null
            ? congregationRepository.findById(t.getFromCongregationId())
                .map(c -> c.getName()).orElse(null)
            : null;
        String toName = congregationRepository.findById(t.getToCongregationId())
            .map(c -> c.getName()).orElse("—");
        return toDto(t, memberName, fromName, toName);
    }

    private MemberTransferDto toDto(MemberTransfer t, String memberName) {
        return toDto(t, memberName, null, null);
    }

    private MemberTransferDto toDto(MemberTransfer t, String memberName, String fromName, String toName) {
        return MemberTransferDto.builder()
            .id(t.getId())
            .memberId(t.getMemberId())
            .memberName(memberName)
            .fromCongregationId(t.getFromCongregationId())
            .fromCongregationName(fromName)
            .toCongregationId(t.getToCongregationId())
            .toCongregationName(toName)
            .reason(t.getReason())
            .transferredAt(t.getTransferredAt())
            .build();
    }
}
