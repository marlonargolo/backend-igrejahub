package com.igrejahub.finance.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.finance.dto.FinancialCategoryDto;
import com.igrejahub.finance.entity.FinancialCategory;
import com.igrejahub.finance.entity.FinancialTransaction;
import com.igrejahub.finance.mapper.FinancialCategoryMapper;
import com.igrejahub.finance.repository.FinancialCategoryRepository;
import com.igrejahub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ISOLAMENTO: categorias com church_id = NULL são globais (padrão do sistema).
 * Categorias com church_id preenchido pertencem àquela Igreja.
 * Um usuário vê: categorias globais (null) + categorias da sua Igreja.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialCategoryService {

    private final FinancialCategoryRepository categoryRepository;
    private final FinancialCategoryMapper     categoryMapper;
    private final SecurityUtils               securityUtils;

    public Page<FinancialCategoryDto> getCategories(Pageable pageable) {
        return categoryRepository.findByOrganizationId(TenantContext.getCurrentTenant(), pageable)
            .map(categoryMapper::toDto);
    }

    public List<FinancialCategoryDto> getActiveCategories() {
        Long orgId    = TenantContext.getCurrentTenant();
        Long churchId = securityUtils.getEffectiveChurchId();

        return categoryRepository.findByOrganizationIdAndActiveTrue(orgId).stream()
            // Ver: categorias globais (churchId nulo) + categorias da Igreja do usuário
            .filter(c -> c.getChurchId() == null
                || securityUtils.canViewAll()
                || c.getChurchId().equals(churchId))
            .map(categoryMapper::toDto)
            .collect(Collectors.toList());
    }

    public List<FinancialCategoryDto> getActiveCategoriesByType(String type) {
        Long orgId    = TenantContext.getCurrentTenant();
        Long churchId = securityUtils.getEffectiveChurchId();

        return categoryRepository.findByOrganizationIdAndType(
            orgId, FinancialTransaction.TransactionType.valueOf(type)).stream()
            .filter(c -> c.isActive())
            .filter(c -> c.getChurchId() == null
                || securityUtils.canViewAll()
                || c.getChurchId().equals(churchId))
            .map(categoryMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public FinancialCategoryDto createCategory(String name, String type, String color) {
        Long orgId    = TenantContext.getCurrentTenant();
        Long churchId = securityUtils.getEffectiveChurchId();

        if (categoryRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, name)) {
            throw new BusinessException("Já existe uma categoria com este nome");
        }
        FinancialCategory category = FinancialCategory.builder()
            .name(name)
            .type(FinancialTransaction.TransactionType.valueOf(type))
            .color(color)
            .active(true)
            .build();
        category.setOrganizationId(orgId);
        // Vincular à Igreja do criador (ROOT global cria categoria global com null)
        category.setChurchId(securityUtils.canViewAll() ? null : churchId);
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Transactional
    public FinancialCategoryDto updateCategory(Long id, String name, String color, Boolean active) {
        FinancialCategory category = getOwnedCategory(id);
        if (name != null) category.setName(name);
        if (color != null) category.setColor(color);
        if (active != null) category.setActive(active);
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        FinancialCategory category = getOwnedCategory(id);
        category.setActive(false);
        categoryRepository.save(category);
    }

    private FinancialCategory getOwnedCategory(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        FinancialCategory category = categoryRepository.findByIdAndOrganizationId(id, orgId)
            .orElseThrow(() -> new ResourceNotFoundException("FinancialCategory", id));
        if (!securityUtils.canViewAll()) {
            Long callerChurchId = securityUtils.getEffectiveChurchId();
            if (category.getChurchId() != null
                    && callerChurchId != null
                    && !callerChurchId.equals(category.getChurchId())) {
                throw new BusinessException("Você não tem acesso a esta categoria.");
            }
        }
        return category;
    }
}