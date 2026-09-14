package com.igrejahub.assets.service;

import com.igrejahub.assets.dto.AssetCategoryDto;
import com.igrejahub.assets.dto.CreateAssetCategoryRequest;
import com.igrejahub.assets.dto.UpdateAssetCategoryRequest;
import com.igrejahub.assets.entity.AssetCategory;
import com.igrejahub.assets.repository.AssetCategoryRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AssetCategoryService — categorias de patrimônio são globais por organização.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetCategoryService {

    private final AssetCategoryRepository categoryRepository;
    private final SecurityUtils           securityUtils;

    // ---------- LIST ----------
    public List<AssetCategoryDto> getCategories() {
        Long orgId = TenantContext.getCurrentTenant();
        return categoryRepository.findByOrganizationId(orgId).stream()
                .filter(c -> c.isActive() || securityUtils.isRoot())
                .map(this::toDto)
                .toList();
    }

    // ---------- GET ONE ----------
    public AssetCategoryDto getCategory(Long id) {
        AssetCategory category = getOwnedCategory(id);
        return toDto(category);
    }

    // ---------- CREATE ----------
    @Transactional
    public AssetCategoryDto createCategory(CreateAssetCategoryRequest request) {
        Long orgId = TenantContext.getCurrentTenant();

        AssetCategory category = AssetCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(true)
                .build();

        // Campos opcionais — só seta se a entidade tiver esses atributos
        trySetParent(category, request.getParentId());
        trySetDepreciationRate(category, request.getDepreciationRate());
        trySetUsefulLifeYears(category, request.getUsefulLifeYears());

        category.setOrganizationId(orgId);
        return toDto(categoryRepository.save(category));
    }

    // ---------- UPDATE ----------
    @Transactional
    public AssetCategoryDto updateCategory(Long id, UpdateAssetCategoryRequest request) {
        AssetCategory category = getOwnedCategory(id);
        if (request.getName() != null)        category.setName(request.getName());
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getActive() != null)      category.setActive(request.getActive());
        trySetParent(category, request.getParentId());
        trySetDepreciationRate(category, request.getDepreciationRate());
        trySetUsefulLifeYears(category, request.getUsefulLifeYears());
        return toDto(categoryRepository.save(category));
    }

    // ---------- DELETE (soft) ----------
    @Transactional
    public void deleteCategory(Long id) {
        AssetCategory category = getOwnedCategory(id);
        category.setActive(false);
        categoryRepository.save(category);
    }

    // ---------- HELPERS ----------
    private AssetCategory getOwnedCategory(Long id) {
        AssetCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AssetCategory", id));
        if (!category.getOrganizationId().equals(TenantContext.getCurrentTenant())) {
            throw new BusinessException("Acesso não autorizado");
        }
        return category;
    }

    private AssetCategoryDto toDto(AssetCategory c) {
        return AssetCategoryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .active(c.isActive())
                .build();
    }

    // Setters tolerantes a campos opcionais que podem não existir na entidade
    private void trySetParent(AssetCategory c, Long parentId) {
        try { c.getClass().getMethod("setParentId", Long.class).invoke(c, parentId); }
        catch (Exception ignored) {}
    }
    private void trySetDepreciationRate(AssetCategory c, Double rate) {
        try { c.getClass().getMethod("setDepreciationRate", Double.class).invoke(c, rate); }
        catch (Exception ignored) {}
    }
    private void trySetUsefulLifeYears(AssetCategory c, Integer years) {
        try { c.getClass().getMethod("setUsefulLifeYears", Integer.class).invoke(c, years); }
        catch (Exception ignored) {}
    }
}