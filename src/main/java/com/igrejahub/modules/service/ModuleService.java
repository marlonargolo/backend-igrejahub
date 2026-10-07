package com.igrejahub.modules.service;

import com.igrejahub.audit.service.AuditLogService;
import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.modules.dto.ModuleDto;
import com.igrejahub.modules.entity.ChurchModule;
import com.igrejahub.modules.entity.Module;
import com.igrejahub.modules.repository.ChurchModuleRepository;
import com.igrejahub.modules.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModuleService {

    private final ModuleRepository       moduleRepository;
    private final ChurchModuleRepository churchModuleRepository;
    private final ChurchRepository       churchRepository;
    private final AuditLogService        auditLogService;

    /** Catálogo de módulos do sistema (sem resolver por Igreja). */
    public List<ModuleDto> getCatalog() {
        return moduleRepository.findByActiveTrueOrderByName().stream()
            .map(m -> toDto(m, true))
            .toList();
    }

    /** Módulos resolvidos para uma Igreja específica: ausência de override = habilitado. */
    public List<ModuleDto> getChurchModules(Long churchId) {
        if (!churchRepository.existsById(churchId)) {
            throw new ResourceNotFoundException("Church", churchId);
        }
        Map<Long, Boolean> overrides = churchModuleRepository.findByChurchId(churchId).stream()
            .collect(java.util.stream.Collectors.toMap(ChurchModule::getModuleId, ChurchModule::isEnabled));

        return moduleRepository.findByActiveTrueOrderByName().stream()
            .map(m -> toDto(m, overrides.getOrDefault(m.getId(), true)))
            .toList();
    }

    @Transactional
    public void setChurchModule(Long churchId, Long moduleId, boolean enabled) {
        if (!churchRepository.existsById(churchId)) {
            throw new ResourceNotFoundException("Church", churchId);
        }
        if (!moduleRepository.existsById(moduleId)) {
            throw new ResourceNotFoundException("Module", moduleId);
        }
        ChurchModule cm = churchModuleRepository.findByChurchIdAndModuleId(churchId, moduleId)
            .orElseGet(() -> ChurchModule.builder().churchId(churchId).moduleId(moduleId).build());
        cm.setEnabled(enabled);
        churchModuleRepository.save(cm);
        auditLogService.logAction("SET_CHURCH_MODULE", "CHURCH_MODULE", moduleId, null,
            Map.of("churchId", churchId, "enabled", enabled));
    }

    /** Usado por /auth/me — chaves dos módulos habilitados para a Igreja do usuário logado. */
    public List<String> getEnabledModuleKeys(Long churchId) {
        if (churchId == null) {
            // ROOT sem Igreja selecionada / usuário sem Igreja: sem restrição.
            return moduleRepository.findByActiveTrueOrderByName().stream().map(Module::getKey).toList();
        }
        return getChurchModules(churchId).stream()
            .filter(ModuleDto::isEnabled)
            .map(ModuleDto::getKey)
            .toList();
    }

    private ModuleDto toDto(Module m, boolean enabled) {
        return ModuleDto.builder()
            .id(m.getId()).key(m.getKey()).name(m.getName())
            .description(m.getDescription()).enabled(enabled)
            .build();
    }
}
