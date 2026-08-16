package com.igrejahub.members.service;

import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.members.dto.CreateMemberRequest;
import com.igrejahub.members.dto.MemberDto;
import com.igrejahub.members.dto.UpdateMemberRequest;
import com.igrejahub.members.entity.Member;
import com.igrejahub.members.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final ChurchRepository churchRepository;
    private final CongregationRepository congregationRepository;

    public Page<MemberDto> getMembers(Pageable pageable, Long congregationId, String status, String search) {
        Long organizationId = TenantContext.getCurrentTenant();
        String normalizedSearch = search != null && !search.isEmpty() ? search : null;
        return memberRepository.search(organizationId, congregationId, status, normalizedSearch, pageable)
            .map(this::toDto);
    }

    public MemberDto getMember(Long id) {
        Long organizationId = TenantContext.getCurrentTenant();
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Member", id));
        if (!member.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        return toDto(member);
    }

    @Transactional
    public MemberDto createMember(CreateMemberRequest request) {
        Long organizationId = TenantContext.getCurrentTenant();

        if (request.getChurchId() != null && !churchRepository.existsByOrganizationIdAndId(organizationId, request.getChurchId())) {
            throw new BusinessException("Igreja não encontrada ou não pertence à sua organização");
        }
        if (request.getCongregationId() != null
                && congregationRepository.findByOrganizationIdAndId(organizationId, request.getCongregationId()).isEmpty()) {
            throw new BusinessException("Congregação não encontrada ou não pertence à sua organização");
        }

        Member member = new Member();
        member.setOrganizationId(organizationId);
        member.setChurchId(request.getChurchId());
        member.setCongregationId(request.getCongregationId());
        member.setName(request.getName());
        member.setEmail(request.getEmail());
        member.setPhone(request.getPhone());
        member.setBirthDate(request.getBirthDate());
        member.setGender(request.getGender());
        member.setMaritalStatus(request.getMaritalStatus());
        member.setProfession(request.getProfession());
        member.setBaptismDate(request.getBaptismDate());
        member.setMemberSince(request.getMemberSince() != null ? request.getMemberSince() : LocalDate.now());
        member.setAddress(request.getAddress());
        member.setNotes(request.getNotes());
        member.setRole(request.getRole());
        member.setStatus("ACTIVE");

        member = memberRepository.save(member);
        return toDto(member);
    }

    @Transactional
    public MemberDto updateMember(Long id, UpdateMemberRequest request) {
        Long organizationId = TenantContext.getCurrentTenant();
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Member", id));
        if (!member.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Acesso não autorizado");
        }

        if (request.getName() != null) member.setName(request.getName());
        if (request.getEmail() != null) member.setEmail(request.getEmail());
        if (request.getPhone() != null) member.setPhone(request.getPhone());
        if (request.getBirthDate() != null) member.setBirthDate(request.getBirthDate());
        if (request.getGender() != null) member.setGender(request.getGender());
        if (request.getMaritalStatus() != null) member.setMaritalStatus(request.getMaritalStatus());
        if (request.getProfession() != null) member.setProfession(request.getProfession());
        if (request.getAddress() != null) member.setAddress(request.getAddress());
        if (request.getNotes() != null) member.setNotes(request.getNotes());
        if (request.getRole() != null) member.setRole(request.getRole());
        if (request.getStatus() != null) member.setStatus(request.getStatus());

        member = memberRepository.save(member);
        return toDto(member);
    }

    @Transactional
    public void deleteMember(Long id) {
        Long organizationId = TenantContext.getCurrentTenant();
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Member", id));
        if (!member.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        member.setStatus("INACTIVE");
        memberRepository.save(member);
    }

    private MemberDto toDto(Member entity) {
        if (entity == null) return null;
        MemberDto dto = new MemberDto();
        dto.setId(entity.getId());
        dto.setChurchId(entity.getChurchId());
        dto.setCongregationId(entity.getCongregationId());
        dto.setName(entity.getName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setBirthDate(entity.getBirthDate());
        dto.setGender(entity.getGender());
        dto.setMaritalStatus(entity.getMaritalStatus());
        dto.setProfession(entity.getProfession());
        dto.setBaptismDate(entity.getBaptismDate());
        dto.setMemberSince(entity.getMemberSince());
        dto.setAddress(entity.getAddress());
        dto.setNotes(entity.getNotes());
        dto.setAvatarUrl(entity.getAvatarUrl());
        dto.setRole(entity.getRole());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
