package com.igrejahub.admin.service;

import com.igrejahub.admin.entity.OrganizationSetting;
import com.igrejahub.admin.repository.OrganizationSettingRepository;
import com.igrejahub.audit.service.AuditLogService;
import com.igrejahub.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Configurações globais da organização (administração externa — ROOT).
 * Modelo chave/valor: segurança, política de senha, email, integrações etc.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlobalSettingsService {

    private final OrganizationSettingRepository settingRepository;
    private final AuditLogService auditLogService;

    public Map<String, Object> getSettings() {
        Long orgId = TenantContext.getCurrentTenant();
        Map<String, Object> settings = new HashMap<>();
        for (OrganizationSetting s : settingRepository.findByOrganizationId(orgId)) {
            settings.put(s.getSettingKey(), s.getSettingValue());
        }
        return settings;
    }

    @Transactional
    public Object updateSetting(String key, Object value) {
        Long orgId = TenantContext.getCurrentTenant();
        OrganizationSetting setting = settingRepository.findByOrganizationIdAndSettingKey(orgId, key)
            .orElseGet(() -> OrganizationSetting.builder()
                .organizationId(orgId)
                .settingKey(key)
                .build());
        Object oldValue = setting.getSettingValue();
        setting.setSettingValue(value);
        setting.setUpdatedAt(LocalDateTime.now());
        settingRepository.save(setting);
        auditLogService.logAction("UPDATE_GLOBAL_SETTING", "ORGANIZATION_SETTING", setting.getId(),
            Map.of("key", key, "value", oldValue != null ? oldValue : ""),
            Map.of("key", key, "value", value != null ? value : ""));
        return value;
    }
}
