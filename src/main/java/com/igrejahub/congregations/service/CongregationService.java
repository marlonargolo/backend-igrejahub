package com.igrejahub.congregations.service;

import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.congregations.dto.CongregationDto;
import com.igrejahub.congregations.dto.CreateCongregationRequest;
import com.igrejahub.congregations.dto.UpdateCongregationRequest;
import com.igrejahub.congregations.entity.Congregation;
import com.igrejahub.congregations.mapper.CongregationMapper;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CongregationService {

    private final CongregationRepository congregationRepository;
    private final ChurchRepository       churchRepository;
    private final CongregationMapper     congregationMapper;
    private final SecurityUtils          securityUtils;

    // ── Listagem ──────────────────────────────────────────────────────────────

    public Page<CongregationDto> getCongregations(Pageable pageable, Long churchIdParam, String search) {
        Long orgId             = TenantContext.getCurrentTenant();
        Long effectiveChurchId = securityUtils.getEffectiveChurchId();

        // Parâmetro churchId do request tem prioridade sobre o contexto
        Long filterChurchId = churchIdParam != null ? churchIdParam : effectiveChurchId;

        // ROOT modo global sem churchIdParam → vê todas
        if (securityUtils.canViewAll() && churchIdParam == null) {
            return search != null && !search.isEmpty()
                ? congregationRepository
                    .findByOrganizationIdAndNameContainingIgnoreCase(orgId, search, pageable)
                    .map(congregationMapper::toDto)
                : congregationRepository.findByOrganizationId(orgId, pageable)
                    .map(congregationMapper::toDto);
        }

        // Sem church_id efetivo → vazio
        if (filterChurchId == null) return Page.empty(pageable);

        // Não-ROOT scoped a congregação → só a própria
        Long userCongId = TenantContext.getCurrentCongregationId();
        if (userCongId != null && !securityUtils.isRoot()) {
            return congregationRepository.findByOrganizationIdAndId(orgId, userCongId)
                .filter(c -> c.getChurchId().equals(filterChurchId))
                .map(c -> (Page<CongregationDto>) new PageImpl<>(
                    List.of(congregationMapper.toDto(c)), pageable, 1))
                .orElse(Page.empty(pageable));
        }

        // Admin/Pastor/ROOT com contexto → todas da Igreja
        return search != null && !search.isEmpty()
            ? congregationRepository
                .findByOrganizationIdAndChurchIdAndNameContainingIgnoreCase(
                    orgId, filterChurchId, search, pageable)
                .map(congregationMapper::toDto)
            : congregationRepository
                .findByOrganizationIdAndChurchId(orgId, filterChurchId, pageable)
                .map(congregationMapper::toDto);
    }

    public CongregationDto getCongregation(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        Congregation c = congregationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Congregation", id));
        if (!c.getOrganizationId().equals(orgId)) throw new BusinessException("Acesso não autorizado");
        if (!securityUtils.canViewAll()) assertAccess(c);
        return congregationMapper.toDto(c);
    }

    // ── Criação ───────────────────────────────────────────────────────────────

    @Transactional
    public CongregationDto createCongregation(CreateCongregationRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        Long targetChurchId;

        if (securityUtils.isRoot() && securityUtils.canViewAll()) {
            // ROOT modo global: deve informar churchId explicitamente
            if (request.getChurchId() == null) {
                throw new BusinessException("Selecione a Igreja para a nova congregação.");
            }
            targetChurchId = request.getChurchId();
        } else {
            // ROOT com contexto ou não-ROOT: usa churchId do contexto
            targetChurchId = securityUtils.getEffectiveChurchId();
            if (targetChurchId == null) {
                throw new BusinessException("Seu usuário não está vinculado a nenhuma Igreja.");
            }
        }

        if (!churchRepository.existsByOrganizationIdAndId(orgId, targetChurchId)) {
            throw new BusinessException("Igreja não encontrada.");
        }

        Congregation c = new Congregation();
        c.setOrganizationId(orgId);
        c.setChurchId(targetChurchId);
        c.setName(request.getName());
        c.setCity(request.getCity());
        c.setState(request.getState());
        c.setAddress(request.getAddress());
        c.setPastorId(request.getPastorId());
        c.setImageUrl(request.getImageUrl());
        c.setStatus("ACTIVE");
        c.setLatitude(request.getLatitude());
        c.setLongitude(request.getLongitude());

        return congregationMapper.toDto(congregationRepository.save(c));
    }

    // ── Update / Delete ───────────────────────────────────────────────────────

    @Transactional
    public CongregationDto updateCongregation(Long id, UpdateCongregationRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        Congregation c = congregationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Congregation", id));
        if (!c.getOrganizationId().equals(orgId)) throw new BusinessException("Acesso não autorizado");
        if (!securityUtils.canViewAll()) assertAccess(c);

        if (request.getName()      != null) c.setName(request.getName());
        if (request.getCity()      != null) c.setCity(request.getCity());
        if (request.getState()     != null) c.setState(request.getState());
        if (request.getAddress()   != null) c.setAddress(request.getAddress());
        if (request.getPastorId()  != null) c.setPastorId(request.getPastorId());
        if (request.getImageUrl()  != null) c.setImageUrl(request.getImageUrl());
        if (request.getStatus()    != null) c.setStatus(request.getStatus());
        if (request.getLatitude()  != null) c.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) c.setLongitude(request.getLongitude());

        return congregationMapper.toDto(congregationRepository.save(c));
    }

    @Transactional
    public void deleteCongregation(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        Congregation c = congregationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Congregation", id));
        if (!c.getOrganizationId().equals(orgId)) throw new BusinessException("Acesso não autorizado");
        if (!securityUtils.canViewAll()) assertAccess(c);
        c.setStatus("INACTIVE");
        congregationRepository.save(c);
    }

    // ── Guard ─────────────────────────────────────────────────────────────────

    private void assertAccess(Congregation c) {
        Long eff     = securityUtils.getEffectiveChurchId();
        Long congId  = TenantContext.getCurrentCongregationId();
        if (eff != null && !c.getChurchId().equals(eff)) {
            throw new BusinessException("Você não tem acesso a esta congregação.");
        }
        if (congId != null && !c.getId().equals(congId)) {
            throw new BusinessException("Você não tem acesso a esta congregação.");
        }
    }
}