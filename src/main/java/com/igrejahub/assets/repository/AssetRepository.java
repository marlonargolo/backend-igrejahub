package com.igrejahub.assets.repository;

import com.igrejahub.assets.entity.Asset;
import com.igrejahub.common.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AssetRepository extends BaseRepository<Asset, Long> {
    Page<Asset> findByOrganizationId(Long organizationId, Pageable pageable);
    Optional<Asset> findByOrganizationIdAndId(Long organizationId, Long id);
    Page<Asset> findByOrganizationIdAndStatus(Long organizationId, String status, Pageable pageable);
    Page<Asset> findByOrganizationIdAndDescriptionContainingIgnoreCase(Long organizationId, String description, Pageable pageable);
    Page<Asset> findByOrganizationIdAndCategoryId(Long organizationId, Long categoryId, Pageable pageable);
    long countByOrganizationIdAndStatus(Long organizationId, String status);
    boolean existsByOrganizationIdAndCode(Long organizationId, String code);
}
