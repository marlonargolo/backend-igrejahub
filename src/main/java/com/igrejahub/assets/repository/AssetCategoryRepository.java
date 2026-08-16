package com.igrejahub.assets.repository;

import com.igrejahub.assets.entity.AssetCategory;
import com.igrejahub.common.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetCategoryRepository extends BaseRepository<AssetCategory, Long> {
    List<AssetCategory> findByOrganizationId(Long organizationId);
    Optional<AssetCategory> findByOrganizationIdAndId(Long organizationId, Long id);
    List<AssetCategory> findByOrganizationIdAndActiveTrue(Long organizationId);
    Optional<AssetCategory> findByOrganizationIdAndName(Long organizationId, String name);
    boolean existsByOrganizationIdAndName(Long organizationId, String name);
}
