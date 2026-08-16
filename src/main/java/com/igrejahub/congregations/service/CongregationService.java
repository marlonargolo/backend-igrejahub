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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CongregationService {
    private final CongregationRepository congregationRepository;
    private final ChurchRepository churchRepository;
    private final CongregationMapper congregationMapper;

    public Page<CongregationDto> getCongregations(Pageable pageable, Long churchId, String search) {
        Long orgId = TenantContext.getCurrentTenant();
        if (churchId != null) {
            return congregationRepository.findByOrganizationIdAndChurchId(orgId, churchId, pageable)
                .map(congregationMapper::toDto);
        }
        if (search != null && !search.isEmpty()) {
            return congregationRepository.findByOrganizationIdAndNameContainingIgnoreCase(orgId, search, pageable)
                .map(congregationMapper::toDto);
        }
        return congregationRepository.findByOrganizationId(orgId, pageable)
            .map(congregationMapper::toDto);
    }

    public CongregationDto getCongregation(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        Congregation congregation = congregationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Congregation", id));
        if (!congregation.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        return congregationMapper.toDto(congregation);
    }

    @Transactional
    public CongregationDto createCongregation(CreateCongregationRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        if (!churchRepository.existsByOrganizationIdAndId(orgId, request.getChurchId())) {
            throw new BusinessException("Igreja não encontrada ou não pertence à sua organização");
        }
        Congregation congregation = new Congregation();
        congregation.setOrganizationId(orgId);
        congregation.setChurchId(request.getChurchId());
        congregation.setName(request.getName());
        congregation.setCity(request.getCity());
        congregation.setState(request.getState());
        congregation.setAddress(request.getAddress());
        congregation.setPastorId(request.getPastorId());
        congregation.setImageUrl(request.getImageUrl());
        congregation.setStatus("ACTIVE");
        congregation.setLatitude(request.getLatitude());
        congregation.setLongitude(request.getLongitude());
        congregation = congregationRepository.save(congregation);
        return congregationMapper.toDto(congregation);
    }

    @Transactional
    public CongregationDto updateCongregation(Long id, UpdateCongregationRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        Congregation congregation = congregationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Congregation", id));
        if (!congregation.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        if (request.getChurchId() != null && !request.getChurchId().equals(congregation.getChurchId()) &&
            !churchRepository.existsByOrganizationIdAndId(orgId, request.getChurchId())) {
            throw new BusinessException("Igreja não encontrada ou não pertence à sua organização");
        }
        if (request.getChurchId() != null) congregation.setChurchId(request.getChurchId());
        if (request.getName() != null) congregation.setName(request.getName());
        if (request.getCity() != null) congregation.setCity(request.getCity());
        if (request.getState() != null) congregation.setState(request.getState());
        if (request.getAddress() != null) congregation.setAddress(request.getAddress());
        if (request.getPastorId() != null) congregation.setPastorId(request.getPastorId());
        if (request.getImageUrl() != null) congregation.setImageUrl(request.getImageUrl());
        if (request.getStatus() != null) congregation.setStatus(request.getStatus());
        if (request.getLatitude() != null) congregation.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) congregation.setLongitude(request.getLongitude());
        congregation = congregationRepository.save(congregation);
        return congregationMapper.toDto(congregation);
    }

    @Transactional
    public void deleteCongregation(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        Congregation congregation = congregationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Congregation", id));
        if (!congregation.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        congregation.setStatus("INACTIVE");
        congregationRepository.save(congregation);
    }
}
