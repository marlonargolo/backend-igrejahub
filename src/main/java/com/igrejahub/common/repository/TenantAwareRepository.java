package com.igrejahub.common.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface TenantAwareRepository<T, ID> extends BaseRepository<T, ID> {
    Page<T> findByOrganizationId(Long organizationId, Pageable pageable);
    List<T> findByOrganizationId(Long organizationId);
    Optional<T> findByOrganizationIdAndId(Long organizationId, ID id);
    boolean existsByOrganizationIdAndId(Long organizationId, ID id);
    long countByOrganizationId(Long organizationId);
}