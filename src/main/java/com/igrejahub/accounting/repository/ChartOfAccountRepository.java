package com.igrejahub.accounting.repository;

import com.igrejahub.accounting.entity.ChartOfAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long> {
    List<ChartOfAccount> findByOrganizationIdAndActiveTrueOrderByCode(Long organizationId);
    List<ChartOfAccount> findByOrganizationIdAndParentId(Long organizationId, Long parentId);
    Optional<ChartOfAccount> findByOrganizationIdAndCode(Long organizationId, String code);
    Optional<ChartOfAccount> findByOrganizationIdAndId(Long organizationId, Long id);
    boolean existsByOrganizationIdAndCode(Long organizationId, String code);
    boolean existsByParentId(Long parentId);
}