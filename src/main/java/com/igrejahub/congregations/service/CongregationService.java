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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ISOLAMENTO MULTI-TENANT — Regras de acesso a Congregações:
 *
 *   ROOT                          → todas da organização
 *   Admin / Pastor da Igreja      → todas da SUA igreja (church_id)
 *   Pastor Congregação / Membro   → apenas a SUA congregação (congregation_id)
 *   Sem church_id                 → lista vazia
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CongregationService {

    private final CongregationRepository congregationRepository;
    private final ChurchRepository churchRepository;
    private final CongregationMapper congregationMapper;
    private final SecurityUtils securityUtils;
    private final JdbcTemplate jdbcTemplate;

    // ─── Listagem com escopo ──────────────────────────────────────────────────

    public Page<CongregationDto> getCongregations(Pageable pageable, Long churchIdParam, String search) {
        Long orgId = TenantContext.getCurrentTenant();

        // ROOT: tudo
        if (securityUtils.isRoot()) {
            if (churchIdParam != null) {
                return congregationRepository
                    .findByOrganizationIdAndChurchId(orgId, churchIdParam, pageable)
                    .map(congregationMapper::toDto);
            }
            if (search != null && !search.isEmpty()) {
                return congregationRepository
                    .findByOrganizationIdAndNameContainingIgnoreCase(orgId, search, pageable)
                    .map(congregationMapper::toDto);
            }
            return congregationRepository.findByOrganizationId(orgId, pageable)
                    .map(congregationMapper::toDto);
        }

        Long userChurchId = TenantContext.getCurrentChurchId();
        Long userCongregationId = TenantContext.getCurrentCongregationId();

        // Sem igreja vinculada → lista vazia
        if (userChurchId == null) {
            log.warn("getCongregations: user {} has no churchId — returning empty", TenantContext.getCurrentUserId());
            return Page.empty(pageable);
        }

        // Rejeitar churchId de outra igreja
        if (churchIdParam != null && !churchIdParam.equals(userChurchId)) {
            log.warn("getCongregations: user tried to access church {} but belongs to {}",
                churchIdParam, userChurchId);
            return Page.empty(pageable);
        }

        // Usuário restrito a uma congregação específica
        if (userCongregationId != null) {
            return congregationRepository.findByOrganizationIdAndId(orgId, userCongregationId)
                .map(c -> (Page<CongregationDto>) new PageImpl<>(
                    List.of(congregationMapper.toDto(c)), pageable, 1))
                .orElse(Page.empty(pageable));
        }

        // Usuário com acesso à Igreja toda → todas as congregações da sua igreja
        if (search != null && !search.isEmpty()) {
            return congregationRepository
                .findByOrganizationIdAndChurchIdAndNameContainingIgnoreCase(orgId, userChurchId, search, pageable)
                .map(congregationMapper::toDto);
        }
        return congregationRepository
            .findByOrganizationIdAndChurchId(orgId, userChurchId, pageable)
            .map(congregationMapper::toDto);
    }

    public CongregationDto getCongregation(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        Congregation congregation = congregationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Congregation", id));

        if (!congregation.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado");
        }

        if (!securityUtils.isRoot()) {
            assertCongregationAccess(congregation);
        }

        return congregationMapper.toDto(congregation);
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @Transactional
    public CongregationDto createCongregation(CreateCongregationRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        Long userChurchId = TenantContext.getCurrentChurchId();

        // Não-ROOT só pode criar na própria igreja
        Long targetChurchId = request.getChurchId();
        if (!securityUtils.isRoot()) {
            if (userChurchId == null) throw new BusinessException("Usuário sem Igreja vinculada.");
            if (!userChurchId.equals(targetChurchId)) {
                throw new BusinessException("Você não pode criar congregações em outra Igreja.");
            }
        }

        if (!churchRepository.existsByOrganizationIdAndId(orgId, targetChurchId)) {
            throw new BusinessException("Igreja não encontrada ou não pertence à sua organização");
        }

        // Quota do plano
        assertCongregationQuota(targetChurchId);

        Congregation congregation = new Congregation();
        congregation.setOrganizationId(orgId);
        congregation.setChurchId(targetChurchId);
        congregation.setName(request.getName());
        congregation.setCity(request.getCity());
        congregation.setState(request.getState());
        congregation.setAddress(request.getAddress());
        congregation.setPastorId(request.getPastorId());
        congregation.setImageUrl(request.getImageUrl());
        congregation.setStatus("ACTIVE");
        congregation.setLatitude(request.getLatitude());
        congregation.setLongitude(request.getLongitude());
        return congregationMapper.toDto(congregationRepository.save(congregation));
    }

    @Transactional
    public CongregationDto updateCongregation(Long id, UpdateCongregationRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        Congregation congregation = congregationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Congregation", id));

        if (!congregation.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        if (!securityUtils.isRoot()) {
            assertCongregationAccess(congregation);
        }

        if (request.getChurchId() != null && !request.getChurchId().equals(congregation.getChurchId())) {
            if (!churchRepository.existsByOrganizationIdAndId(orgId, request.getChurchId())) {
                throw new BusinessException("Igreja destino não encontrada");
            }
            congregation.setChurchId(request.getChurchId());
        }
        if (request.getName() != null)      congregation.setName(request.getName());
        if (request.getCity() != null)      congregation.setCity(request.getCity());
        if (request.getState() != null)     congregation.setState(request.getState());
        if (request.getAddress() != null)   congregation.setAddress(request.getAddress());
        if (request.getPastorId() != null)  congregation.setPastorId(request.getPastorId());
        if (request.getImageUrl() != null)  congregation.setImageUrl(request.getImageUrl());
        if (request.getStatus() != null)    congregation.setStatus(request.getStatus());
        if (request.getLatitude() != null)  congregation.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) congregation.setLongitude(request.getLongitude());

        return congregationMapper.toDto(congregationRepository.save(congregation));
    }

    @Transactional
    public void deleteCongregation(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        Congregation congregation = congregationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Congregation", id));

        if (!congregation.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        if (!securityUtils.isRoot()) {
            assertCongregationAccess(congregation);
        }
        congregation.setStatus("INACTIVE");
        congregationRepository.save(congregation);
    }

    // ─── Guards e helpers ─────────────────────────────────────────────────────

    private void assertCongregationAccess(Congregation congregation) {
        Long userChurchId = TenantContext.getCurrentChurchId();
        Long userCongId   = TenantContext.getCurrentCongregationId();

        // Igreja diferente → bloqueia sempre
        if (!congregation.getChurchId().equals(userChurchId)) {
            throw new BusinessException("Você não tem acesso a esta congregação.");
        }

        // Restrito a congregação específica → deve ser a mesma
        if (userCongId != null && !congregation.getId().equals(userCongId)) {
            throw new BusinessException("Você não tem acesso a esta congregação.");
        }
    }

    private void assertCongregationQuota(Long churchId) {
        List<Long> maxList = jdbcTemplate.queryForList(
            "SELECT p.max_congregations FROM plans p " +
            "JOIN churches c ON c.plan_id = p.id WHERE c.id = ?",
            Long.class, churchId
        );
        if (maxList.isEmpty()) return;
        long max = maxList.get(0);
        long current = congregationRepository.countByChurchId(churchId);
        if (current >= max) {
            throw new BusinessException(
                "Limite de congregações do plano atingido (" + max + "). Solicite upgrade.");
        }
    }
}