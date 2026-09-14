package com.igrejahub.finance.service;

import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.finance.mapper.FinancialCategoryMapper;
import com.igrejahub.finance.repository.FinancialCategoryRepository;
import com.igrejahub.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialCategoryServiceIsolationTest {

    @Mock private FinancialCategoryRepository categoryRepository;
    @Mock private FinancialCategoryMapper categoryMapper;
    @Mock private SecurityUtils securityUtils;

    private final Pageable pageable = PageRequest.of(0, 20);

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private FinancialCategoryService newService() {
        return new FinancialCategoryService(categoryRepository, categoryMapper, securityUtils);
    }

    @Test
    void getCategories_churchScopedUser_seesOnlyGlobalPlusOwnChurch() {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(categoryRepository.findVisible(1L, false, 10L, pageable)).thenReturn(Page.empty(pageable));

        newService().getCategories(pageable);

        verify(categoryRepository).findVisible(1L, false, 10L, pageable);
    }

    @Test
    void getCategories_rootGlobal_ignoresChurchFilter() {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.canViewAll()).thenReturn(true);
        when(categoryRepository.findVisible(eq(1L), eq(true), eq(null), eq(pageable)))
            .thenReturn(Page.empty(pageable));

        newService().getCategories(pageable);

        verify(categoryRepository).findVisible(1L, true, null, pageable);
    }
}
