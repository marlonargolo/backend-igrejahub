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
public class ChurchService {

    private final ChurchRepository churchRepository;
    private final ChurchMapper churchMapper;

    public Page<ChurchDto> getChurches(Pageable pageable, String search) {
        Long orgId = TenantContext.getCurrentTenant();
        if (search != null && !search.isEmpty()) {
            return churchRepository.findByOrganizationIdAndNameContainingIgnoreCase(orgId, search, pageable)
                    .map(churchMapper::toDto);
        }
        return churchRepository.findByOrganizationId(orgId, pageable).map(churchMapper::toDto);
    }

    public ChurchDto getChurch(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        Church church = churchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Church", id));
        if (!church.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        return churchMapper.toDto(church);
    }

    @Transactional
    public ChurchDto createChurch(CreateChurchRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        if (request.getCnpj() != null && !request.getCnpj().isBlank()
                && churchRepository.existsByOrganizationIdAndCnpj(orgId, request.getCnpj())) {
            throw new BusinessException("Já existe uma igreja com este CNPJ nesta organização");
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
                .status("ACTIVE")
                .build();
        church.setOrganizationId(orgId);
        church = churchRepository.save(church);
        return churchMapper.toDto(church);
    }

    @Transactional
    public ChurchDto updateChurch(Long id, UpdateChurchRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        Church church = churchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Church", id));
        if (!church.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        if (request.getName() != null) church.setName(request.getName());
        if (request.getCity() != null) church.setCity(request.getCity());
        if (request.getState() != null) church.setState(request.getState());
        if (request.getAddress() != null) church.setAddress(request.getAddress());
        if (request.getZipCode() != null) church.setZipCode(request.getZipCode());
        if (request.getPhone() != null) church.setPhone(request.getPhone());
        if (request.getEmail() != null) church.setEmail(request.getEmail());
        if (request.getCnpj() != null) church.setCnpj(request.getCnpj());
        if (request.getLogoUrl() != null) church.setLogoUrl(request.getLogoUrl());
        if (request.getStatus() != null) church.setStatus(request.getStatus());
        if (request.getPastorId() != null) church.setPastorId(request.getPastorId());
        church = churchRepository.save(church);
        return churchMapper.toDto(church);
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