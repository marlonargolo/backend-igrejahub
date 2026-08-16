package com.igrejahub.organizations.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.exception.TenantAccessDeniedException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.organizations.dto.CreateOrganizationRequest;
import com.igrejahub.organizations.dto.OrganizationDto;
import com.igrejahub.organizations.dto.UpdateOrganizationRequest;
import com.igrejahub.organizations.entity.Organization;
import com.igrejahub.organizations.mapper.OrganizationMapper;
import com.igrejahub.organizations.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;

    @Cacheable(value = "organizations", key = "#id")
    public OrganizationDto getOrganization(Long id) {
        return organizationMapper.toDto(getOwnedOrganization(id));
    }

    public Page<OrganizationDto> getOrganizations(Pageable pageable) {
        OrganizationDto current = organizationMapper.toDto(getCurrentOrganization());
        return new PageImpl<>(List.of(current), pageable, 1);
    }

    @Transactional
    @CacheEvict(value = "organizations", key = "#result.id")
    public OrganizationDto createOrganization(CreateOrganizationRequest request) {
        if (request.getCnpj() != null && !request.getCnpj().isEmpty() &&
            organizationRepository.existsByCnpj(request.getCnpj())) {
            throw new BusinessException("CNPJ já cadastrado");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty() &&
            organizationRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email já cadastrado");
        }
        
        Organization org = new Organization();
        org.setName(request.getName());
        org.setLegalName(request.getLegalName());
        org.setCnpj(request.getCnpj());
        org.setEmail(request.getEmail());
        org.setPhone(request.getPhone());
        org.setAddress(request.getAddress());
        org.setCity(request.getCity());
        org.setState(request.getState());
        org.setZipCode(request.getZipCode());
        org.setCountry(request.getCountry() != null ? request.getCountry() : "BR");
        org.setActive(true);
        org.setPlan("FREE");
        
        org = organizationRepository.save(org);
        return organizationMapper.toDto(org);
    }

    @Transactional
    @CacheEvict(value = "organizations", key = "#id")
    public OrganizationDto updateOrganization(Long id, UpdateOrganizationRequest request) {
        Organization org = getOwnedOrganization(id);

        if (request.getCnpj() != null && !request.getCnpj().isEmpty() &&
            !request.getCnpj().equals(org.getCnpj()) &&
            organizationRepository.existsByCnpj(request.getCnpj())) {
            throw new BusinessException("CNPJ já cadastrado");
        }
        
        if (request.getName() != null) org.setName(request.getName());
        if (request.getLegalName() != null) org.setLegalName(request.getLegalName());
        if (request.getCnpj() != null) org.setCnpj(request.getCnpj());
        if (request.getEmail() != null) org.setEmail(request.getEmail());
        if (request.getPhone() != null) org.setPhone(request.getPhone());
        if (request.getAddress() != null) org.setAddress(request.getAddress());
        if (request.getCity() != null) org.setCity(request.getCity());
        if (request.getState() != null) org.setState(request.getState());
        if (request.getZipCode() != null) org.setZipCode(request.getZipCode());
        if (request.getCountry() != null) org.setCountry(request.getCountry());
        if (request.getLogoUrl() != null) org.setLogoUrl(request.getLogoUrl());
        if (request.getPlan() != null) org.setPlan(request.getPlan());
        if (request.getActive() != null) org.setActive(request.getActive());
        
        org = organizationRepository.save(org);
        return organizationMapper.toDto(org);
    }

    @Transactional
    @CacheEvict(value = "organizations", key = "#id")
    public void deleteOrganization(Long id) {
        Organization org = getOwnedOrganization(id);
        org.setActive(false);
        organizationRepository.save(org);
    }

    public Organization getCurrentOrganization() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) throw new IllegalStateException("No tenant context set");
        return organizationRepository.findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", tenantId));
    }

    private Organization getOwnedOrganization(Long id) {
        Organization org = organizationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", id));
        if (!org.getId().equals(TenantContext.getCurrentTenant())) {
            throw new TenantAccessDeniedException();
        }
        return org;
    }
}
