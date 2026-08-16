package com.igrejahub.assets.service;

import com.igrejahub.assets.dto.AssetDto;
import com.igrejahub.assets.dto.CreateAssetRequest;
import com.igrejahub.assets.dto.UpdateAssetRequest;
import com.igrejahub.assets.entity.Asset;
import com.igrejahub.assets.mapper.AssetMapper;
import com.igrejahub.assets.repository.AssetRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {
    private final AssetRepository assetRepository;
    private final AssetMapper assetMapper;

    public Page<AssetDto> getAssets(Pageable pageable, Long categoryId, String search, String status) {
        Long organizationId = TenantContext.getCurrentTenant();
        if (categoryId != null) {
            return assetRepository.findByOrganizationIdAndCategoryId(organizationId, categoryId, pageable)
                .map(assetMapper::toDto);
        }
        if (search != null && !search.isEmpty()) {
            return assetRepository.findByOrganizationIdAndDescriptionContainingIgnoreCase(organizationId, search, pageable)
                .map(assetMapper::toDto);
        }
        if (status != null) {
            return assetRepository.findByOrganizationIdAndStatus(organizationId, status, pageable)
                .map(assetMapper::toDto);
        }
        return assetRepository.findByOrganizationId(organizationId, pageable)
            .map(assetMapper::toDto);
    }

    public AssetDto getAsset(Long id) {
        Long organizationId = TenantContext.getCurrentTenant();
        Asset asset = assetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
        if (!asset.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        return assetMapper.toDto(asset);
    }

    @Transactional
    public AssetDto createAsset(CreateAssetRequest request) {
        Long organizationId = TenantContext.getCurrentTenant();
        if (request.getCode() != null && !request.getCode().isEmpty()) {
            if (assetRepository.existsByOrganizationIdAndCode(organizationId, request.getCode())) {
                throw new BusinessException("Código do patrimônio já existe");
            }
        }
        Asset asset = new Asset();
        asset.setOrganizationId(organizationId);
        asset.setChurchId(request.getChurchId());
        asset.setCongregationId(request.getCongregationId());
        asset.setCode(request.getCode());
        asset.setDescription(request.getDescription());
        asset.setCategoryId(request.getCategoryId());
        asset.setAcquisitionDate(request.getAcquisitionDate());
        asset.setLocation(request.getLocation());
        asset.setStatus("ACTIVE");
        asset.setNotes(request.getNotes());
        asset.setSerialNumber(request.getSerialNumber());
        asset.setManufacturer(request.getManufacturer());
        asset.setModel(request.getModel());
        asset.setWarrantyEndDate(request.getWarrantyEndDate());
        
        if (request.getOriginalValue() != null) {
            long cents = toCents(request.getOriginalValue());
            asset.setOriginalValueCents(cents);
            asset.setCurrentValueCents(cents);
        }
        
        asset = assetRepository.save(asset);
        return assetMapper.toDto(asset);
    }

    @Transactional
    public AssetDto updateAsset(Long id, UpdateAssetRequest request) {
        Long organizationId = TenantContext.getCurrentTenant();
        Asset asset = assetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
            
        if (!asset.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Acesso não autorizado");
        }

        if (request.getCode() != null && !request.getCode().isEmpty() &&
            !request.getCode().equals(asset.getCode()) &&
            assetRepository.existsByOrganizationIdAndCode(organizationId, request.getCode())) {
            throw new BusinessException("Código do patrimônio já existe");
        }

        if (request.getChurchId() != null) asset.setChurchId(request.getChurchId());
        if (request.getCongregationId() != null) asset.setCongregationId(request.getCongregationId());
        if (request.getCode() != null) asset.setCode(request.getCode());
        if (request.getDescription() != null) asset.setDescription(request.getDescription());
        if (request.getCategoryId() != null) asset.setCategoryId(request.getCategoryId());
        if (request.getAcquisitionDate() != null) asset.setAcquisitionDate(request.getAcquisitionDate());
        if (request.getLocation() != null) asset.setLocation(request.getLocation());
        if (request.getStatus() != null) asset.setStatus(request.getStatus());
        if (request.getNotes() != null) asset.setNotes(request.getNotes());
        if (request.getSerialNumber() != null) asset.setSerialNumber(request.getSerialNumber());
        if (request.getManufacturer() != null) asset.setManufacturer(request.getManufacturer());
        if (request.getModel() != null) asset.setModel(request.getModel());
        if (request.getWarrantyEndDate() != null) asset.setWarrantyEndDate(request.getWarrantyEndDate());
        
        if (request.getOriginalValue() != null) {
            asset.setOriginalValueCents(toCents(request.getOriginalValue()));
        }
        if (request.getCurrentValue() != null) {
            asset.setCurrentValueCents(toCents(request.getCurrentValue()));
        }

        asset = assetRepository.save(asset);
        return assetMapper.toDto(asset);
    }

    @Transactional
    public void deleteAsset(Long id) {
        Long organizationId = TenantContext.getCurrentTenant();
        Asset asset = assetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
        if (!asset.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        asset.setStatus("WRITTEN_OFF");
        assetRepository.save(asset);
    }

    @Transactional
    public void writeOffAsset(Long id, String reason) {
        Long organizationId = TenantContext.getCurrentTenant();
        Asset asset = assetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
        if (!asset.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        asset.setStatus("WRITTEN_OFF");
        asset.setNotes((asset.getNotes() != null ? asset.getNotes() + " " : "") + "Baixado: " + reason);
        assetRepository.save(asset);
    }

    private long toCents(Double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }
}
