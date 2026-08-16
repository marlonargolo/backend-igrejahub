package com.igrejahub.finance.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.finance.dto.FinancialCategoryDto;
import com.igrejahub.finance.entity.FinancialCategory;
import com.igrejahub.finance.entity.FinancialTransaction;
import com.igrejahub.finance.mapper.FinancialCategoryMapper;
import com.igrejahub.finance.repository.FinancialCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialCategoryService {

    private final FinancialCategoryRepository categoryRepository;
    private final FinancialCategoryMapper categoryMapper;

    public Page<FinancialCategoryDto> getCategories(Pageable pageable) {
        return categoryRepository.findByOrganizationId(TenantContext.getCurrentTenant(), pageable)
                .map(categoryMapper::toDto);
    }

    public List<FinancialCategoryDto> getActiveCategories() {
        return categoryRepository.findByOrganizationIdAndActiveTrue(TenantContext.getCurrentTenant())
                .stream().map(categoryMapper::toDto).toList();
    }

    @Transactional
    public FinancialCategoryDto createCategory(String name, String type, String color) {
        Long orgId = TenantContext.getCurrentTenant();
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
        FinancialCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialCategory", id));
        if (!category.getOrganizationId().equals(TenantContext.getCurrentTenant())) {
            throw new BusinessException("Acesso não autorizado");
        }
        return category;
    }
}