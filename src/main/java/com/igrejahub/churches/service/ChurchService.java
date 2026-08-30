package com.igrejahub.churches.service;

import com.igrejahub.churches.dto.ChurchDto;
import com.igrejahub.churches.dto.CreateChurchRequest;
import com.igrejahub.churches.dto.UpdateChurchRequest;
import com.igrejahub.churches.entity.Church;
import com.igrejahub.churches.mapper.ChurchMapper;
import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.plans.entity.Plan;
import com.igrejahub.plans.repository.PlanRepository;
import com.igrejahub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ISOLAMENTO MULTI-TENANT — Regras de acesso a Igrejas:
 *
 *   ROOT               → todas as igrejas da organização
 *   Admin/Usuário      → apenas a própria igreja (church_id do usuário)
 *   Sem church_id      → lista vazia (nunca expõe dados de outra igreja)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChurchService {

    private final ChurchRepository churchRepository;
    private final ChurchMapper churchMapper;
    private final PlanRepository planRepository;
    private final SecurityUtils securityUtils;

    // ─── Listagem com escopo ──────────────────────────────────────────────────

    public Page<ChurchDto> getChurches(Pageable pageable, String search) {
        Long orgId = TenantContext.getCurrentTenant();

        // ROOT vê todas
        if (securityUtils.isRoot()) {
            if (search != null && !search.isEmpty()) {
                return churchRepository
                    .findByOrganizationIdAndNameContainingIgnoreCase(orgId, search, pageable)
                    .map(churchMapper::toDto);
            }
            return churchRepository.findByOrganizationId(orgId, pageable).map(churchMapper::toDto);
        }

        // Todos os outros: apenas a própria igreja
        Long userChurchId = TenantContext.getCurrentChurchId();
        if (userChurchId == null) {
            log.warn("getChurches: user {} has no churchId — returning empty", TenantContext.getCurrentUserId());
            return Page.empty(pageable);
        }

        return churchRepository.findByOrganizationIdAndId(orgId, userChurchId)
            .map(c -> {
                if (search != null && !search.isEmpty()
                        && !c.getName().toLowerCase().contains(search.toLowerCase())) {
                    return new PageImpl<ChurchDto>(List.of(), pageable, 0);
                }
                return (Page<ChurchDto>) new PageImpl<>(List.of(churchMapper.toDto(c)), pageable, 1);
            })
            .orElse(Page.empty(pageable));
    }

    public ChurchDto getChurch(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        Church church = churchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Church", id));

        if (!church.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado");
        }

        // Não-ROOT só pode acessar a própria igreja
        if (!securityUtils.isRoot()) {
            Long userChurchId = TenantContext.getCurrentChurchId();
            if (!id.equals(userChurchId)) {
                throw new BusinessException("Você não tem acesso a esta Igreja.");
            }
        }

        return churchMapper.toDto(church);
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @Transactional
    public ChurchDto createChurch(CreateChurchRequest request) {
        Long orgId = TenantContext.getCurrentTenant();

        if (request.getCnpj() != null && !request.getCnpj().isBlank()
                && churchRepository.existsByOrganizationIdAndCnpj(orgId, request.getCnpj())) {
            throw new BusinessException("Já existe uma igreja com este CNPJ nesta organização");
        }

        Plan plan = null;
        if (request.getPlanId() != null) {
            plan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new BusinessException("Plano não encontrado: " + request.getPlanId()));
        }

        Church church = Church.builder()
                .name(request.getName())
                .city(request.getCity())
                .state(request.getState())
                .address(request.getAddress())
                .zipCode(request.getZipCode())
                .phone(request.getPhone())
                .email(request.getEmail())
                .cnpj(request.getCnpj())
                .logoUrl(request.getLogoUrl())
                .pastorId(request.getPastorId())
                .plan(plan)
                .status("ACTIVE")
                .build();
        church.setOrganizationId(orgId);
        return churchMapper.toDto(churchRepository.save(church));
    }

    @Transactional
    public ChurchDto updateChurch(Long id, UpdateChurchRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        Church church = churchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Church", id));

        if (!church.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        if (!securityUtils.isRoot()) {
            Long userChurchId = TenantContext.getCurrentChurchId();
            if (!id.equals(userChurchId)) {
                throw new BusinessException("Você não pode editar outra Igreja.");
            }
        }

        if (request.getName() != null)     church.setName(request.getName());
        if (request.getCity() != null)     church.setCity(request.getCity());
        if (request.getState() != null)    church.setState(request.getState());
        if (request.getAddress() != null)  church.setAddress(request.getAddress());
        if (request.getZipCode() != null)  church.setZipCode(request.getZipCode());
        if (request.getPhone() != null)    church.setPhone(request.getPhone());
        if (request.getEmail() != null)    church.setEmail(request.getEmail());
        if (request.getCnpj() != null)     church.setCnpj(request.getCnpj());
        if (request.getLogoUrl() != null)  church.setLogoUrl(request.getLogoUrl());
        if (request.getStatus() != null)   church.setStatus(request.getStatus());
        if (request.getPastorId() != null) church.setPastorId(request.getPastorId());

        // Troca de plano: apenas ROOT
        if (request.getPlanId() != null) {
            if (!securityUtils.isRoot()) throw new BusinessException("Apenas ROOT pode trocar o plano.");
            Plan plan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new BusinessException("Plano não encontrado"));
            church.setPlan(plan);
        }

        return churchMapper.toDto(churchRepository.save(church));
    }

    @Transactional
    public void deleteChurch(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        Church church = churchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Church", id));
        if (!church.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        church.setStatus("INACTIVE");
        churchRepository.save(church);
    }
}