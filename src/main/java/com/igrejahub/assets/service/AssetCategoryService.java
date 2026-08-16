package com.igrejahub.assets.service;

import com.igrejahub.assets.dto.AssetCategoryDto;
import com.igrejahub.assets.dto.CreateAssetCategoryRequest;
import com.igrejahub.assets.entity.AssetCategory;
import com.igrejahub.assets.mapper.AssetCategoryMapper;
import com.igrejahub.assets.repository.AssetCategoryRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetCategoryService {

    private final AssetCategoryRepository categoryRepository;
    private final AssetCategoryMapper categoryMapper;

    public List<AssetCategoryDto> getCategories() {
        Long orgId = TenantContext.getCurrentTenant();
        return categoryRepository.findByOrganizationId(orgId).stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    public AssetCategoryDto getCategory(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        AssetCategory category = categoryRepository.findByOrganizationIdAndId(orgId, id)
                .orElseThrow(() -> new ResourceNotFoundException("AssetCategory", id));
        return categoryMapper.toDto(category);
    }

    @Transactional
    public AssetCategoryDto createCategory(CreateAssetCategoryRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        if (categoryRepository.existsByOrganizationIdAndName(orgId, request.getName())) {
            throw new BusinessException("Categoria de patrimônio já existe");
        }
        AssetCategory category = new AssetCategory();
        category.setOrganizationId(orgId);
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setParentId(request.getParentId());
        category.setDepreciationRate(request.getDepreciationRate());
        category.setUsefulLifeYears(request.getUsefulLifeYears());
        category.setActive(true);
        return categoryMapper.toDto(categoryRepository.save(category));
    }
}
