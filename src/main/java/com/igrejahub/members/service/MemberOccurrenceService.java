package com.igrejahub.members.service;

import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.members.dto.CreateOccurrenceRequest;
import com.igrejahub.members.dto.MemberOccurrenceDto;
import com.igrejahub.members.entity.MemberOccurrence;
import com.igrejahub.members.repository.MemberOccurrenceRepository;
import com.igrejahub.members.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberOccurrenceService {

    private final MemberOccurrenceRepository occurrenceRepository;
    private final MemberRepository memberRepository;

    public List<MemberOccurrenceDto> getOccurrences(Long memberId) {
        memberRepository.findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
        return occurrenceRepository
            .findByMemberIdAndDeletedFalseOrderByOccurrenceDateDesc(memberId)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public MemberOccurrenceDto createOccurrence(Long memberId, CreateOccurrenceRequest request) {
        memberRepository.findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
        MemberOccurrence o = MemberOccurrence.builder()
            .organizationId(TenantContext.getCurrentTenant())
            .memberId(memberId)
            .occurrenceDate(request.getOccurrenceDate())
            .description(request.getDescription())
            .build();
        return toDto(occurrenceRepository.save(o));
    }

    @Transactional
    public void deleteOccurrence(Long occurrenceId) {
        MemberOccurrence o = occurrenceRepository.findById(occurrenceId)
            .orElseThrow(() -> new ResourceNotFoundException("Occurrence", occurrenceId));
        o.setDeleted(true);
        occurrenceRepository.save(o);
    }

    private MemberOccurrenceDto toDto(MemberOccurrence e) {
        return MemberOccurrenceDto.builder()
            .id(e.getId())
            .memberId(e.getMemberId())
            .occurrenceDate(e.getOccurrenceDate())
            .description(e.getDescription())
            .createdAt(e.getCreatedAt())
            .build();
    }
}