package com.igrejahub.admin.repository;

import com.igrejahub.admin.entity.OrganizationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationSettingRepository extends JpaRepository<OrganizationSetting, Long> {
    List<OrganizationSetting> findByOrganizationId(Long organizationId);
    Optional<OrganizationSetting> findByOrganizationIdAndSettingKey(Long organizationId, String settingKey);
}
