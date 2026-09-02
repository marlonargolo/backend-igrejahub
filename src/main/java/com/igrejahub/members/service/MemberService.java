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
import com.igrejahub.security.SecurityUtils;
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

    private final MemberRepository       memberRepository;
    private final ChurchRepository       churchRepository;
    private final CongregationRepository congregationRepository;
    private final SecurityUtils          securityUtils;

    public Page<MemberDto> getMembers(Pageable pageable, Long churchIdParam,
                                      Long congregationId, String status, String search) {
        Long orgId  = TenantContext.getCurrentTenant();
        String s    = search != null && !search.isEmpty() ? search : null;
        Long effChurchId    = securityUtils.getEffectiveChurchId();
        Long effCongId      = TenantContext.getCurrentCongregationId();

        log.debug("getMembers orgId={} effChurchId={} effCongId={} canViewAll={}",
            orgId, effChurchId, effCongId, securityUtils.canViewAll());

        // ROOT modo global → todos da organização
        if (securityUtils.canViewAll()) {
            return memberRepository.findAllByOrganization(orgId, status, s, pageable)
                .map(this::toDto);
        }

        // Pastor de Congregação → só da sua congregação
        if (effCongId != null && !securityUtils.isRoot()) {
            return memberRepository.findByCongregationId(orgId, effCongId, status, s, pageable)
                .map(this::toDto);
        }

        // Admin/Pastor da Igreja ou ROOT com contexto → filtra por churchId
        if (effChurchId == null) return Page.empty(pageable);

        return memberRepository.findByChurchId(orgId, effChurchId, congregationId, status, s, pageable)
            .map(this::toDto);
    }

    public MemberDto getMember(Long id) {
        Long organizationId = TenantContext.getCurrentTenant();
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Member", id));
        if (!member.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        // Validar isolamento
        if (!securityUtils.canViewAll()) {
            Long eff = securityUtils.getEffectiveChurchId();
            if (eff != null && !eff.equals(member.getChurchId())) {
                throw new BusinessException("Você não tem acesso a este membro.");
            }
        }
        return toDto(member);
    }

    @Transactional
    public MemberDto createMember(CreateMemberRequest request) {
        Long organizationId = TenantContext.getCurrentTenant();

        // Determinar Igreja alvo
        Long targetChurchId;
        if (securityUtils.canViewAll()) {
            targetChurchId = request.getChurchId();
            if (targetChurchId == null) {
                throw new BusinessException("Selecione a Igreja para o novo membro.");
            }
        } else {
            Long effChurchId = securityUtils.getEffectiveChurchId();
            if (effChurchId == null) {
                throw new BusinessException("Seu usuário não está vinculado a nenhuma Igreja.");
            }
            // Não pode criar em outra Igreja
            if (request.getChurchId() != null && !request.getChurchId().equals(effChurchId)) {
                throw new BusinessException("Você não pode criar membros em outra Igreja.");
            }
            targetChurchId = effChurchId;
        }

        if (!churchRepository.existsByOrganizationIdAndId(organizationId, targetChurchId)) {
            throw new BusinessException("Igreja não encontrada ou não pertence à sua organização.");
        }

        // Congregação deve pertencer à Igreja
        Long targetCongId = request.getCongregationId();
        Long effCongId    = TenantContext.getCurrentCongregationId();
        if (effCongId != null && !securityUtils.isRoot()) {
            // Pastor de Congregação: forçar para a sua congregação
            targetCongId = effCongId;
        } else if (targetCongId != null
                && congregationRepository.findByOrganizationIdAndId(organizationId, targetCongId).isEmpty()) {
            throw new BusinessException("Congregação não encontrada ou não pertence à sua organização.");
        }

        Member member = new Member();
        member.setOrganizationId(organizationId);
        member.setChurchId(targetChurchId);
        member.setCongregationId(targetCongId);
        member.setName(request.getName());
        member.setEmail(request.getEmail());
        member.setPhone(request.getPhone());
        member.setRg(request.getRg());
        member.setCpf(request.getCpf());
        member.setBirthDate(request.getBirthDate());
        member.setGender(request.getGender());
        member.setMaritalStatus(request.getMaritalStatus());
        member.setProfession(request.getProfession());
        member.setBaptismDate(request.getBaptismDate());
        member.setMemberSince(request.getMemberSince() != null ? request.getMemberSince() : LocalDate.now());
        member.setAddress(request.getAddress());
        member.setNotes(request.getNotes());
        member.setCargo(request.getCargo());
        member.setFuncoes(request.getFuncoes());
        member.setRole(request.getCargo());
        member.setStatus("ACTIVE");

        member = memberRepository.save(member);
        log.info("Member created: {} churchId={} congregationId={} by={}",
            member.getName(), member.getChurchId(), member.getCongregationId(),
            TenantContext.getCurrentUserId());
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

        if (request.getName()          != null) member.setName(request.getName());
        if (request.getEmail()         != null) member.setEmail(request.getEmail());
        if (request.getPhone()         != null) member.setPhone(request.getPhone());
        if (request.getRg()            != null) member.setRg(request.getRg());
        if (request.getCpf()           != null) member.setCpf(request.getCpf());
        if (request.getBirthDate()     != null) member.setBirthDate(request.getBirthDate());
        if (request.getGender()        != null) member.setGender(request.getGender());
        if (request.getMaritalStatus() != null) member.setMaritalStatus(request.getMaritalStatus());
        if (request.getProfession()    != null) member.setProfession(request.getProfession());
        if (request.getAddress()       != null) member.setAddress(request.getAddress());
        if (request.getNotes()         != null) member.setNotes(request.getNotes());
        if (request.getCargo()         != null) { member.setCargo(request.getCargo()); member.setRole(request.getCargo()); }
        if (request.getFuncoes()       != null) member.setFuncoes(request.getFuncoes());
        if (request.getStatus()        != null) member.setStatus(request.getStatus());
        if (request.getCongregationId() != null) member.setCongregationId(request.getCongregationId());

        return toDto(memberRepository.save(member));
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
        dto.setRg(entity.getRg());
        dto.setCpf(entity.getCpf());
        dto.setBirthDate(entity.getBirthDate());
        dto.setGender(entity.getGender());
        dto.setMaritalStatus(entity.getMaritalStatus());
        dto.setProfession(entity.getProfession());
        dto.setBaptismDate(entity.getBaptismDate());
        dto.setMemberSince(entity.getMemberSince());
        dto.setAddress(entity.getAddress());
        dto.setNotes(entity.getNotes());
        dto.setAvatarUrl(entity.getAvatarUrl());
        dto.setCargo(entity.getCargo());
        dto.setFuncoes(entity.getFuncoes());
        dto.setRole(entity.getRole());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    @Transactional
    public void updateAvatar(Long id, String avatarUrl) {
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Member", id));
        member.setAvatarUrl(avatarUrl);
        memberRepository.save(member);
    }
}