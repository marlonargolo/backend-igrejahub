package com.igrejahub.modules.service;

import com.igrejahub.audit.service.AuditLogService;
import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.modules.dto.ModuleDto;
import com.igrejahub.modules.entity.ChurchModule;
import com.igrejahub.modules.entity.Module;
import com.igrejahub.modules.repository.ChurchModuleRepository;
import com.igrejahub.modules.repository.ModuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Módulos por Igreja: ausência de override em church_modules precisa
 * significar "habilitado" — preserva o comportamento atual de quem já
 * existe quando o catálogo é introduzido.
 */
@ExtendWith(MockitoExtension.class)
class ModuleServiceTest {

    @Mock private ModuleRepository moduleRepository;
    @Mock private ChurchModuleRepository churchModuleRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private AuditLogService auditLogService;

    private ModuleService newService() {
        return new ModuleService(moduleRepository, churchModuleRepository, churchRepository, auditLogService);
    }

    @Test
    void getChurchModules_withoutOverride_defaultsToEnabled() {
        when(churchRepository.existsById(10L)).thenReturn(true);
        Module secretaria = Module.builder().id(1L).key("SECRETARIA").name("Secretaria").active(true).build();
        when(moduleRepository.findByActiveTrueOrderByName()).thenReturn(List.of(secretaria));
        when(churchModuleRepository.findByChurchId(10L)).thenReturn(List.of());

        List<ModuleDto> result = newService().getChurchModules(10L);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isEnabled());
    }

    @Test
    void getChurchModules_withDisabledOverride_reflectsIt() {
        when(churchRepository.existsById(10L)).thenReturn(true);
        Module secretaria = Module.builder().id(1L).key("SECRETARIA").name("Secretaria").active(true).build();
        when(moduleRepository.findByActiveTrueOrderByName()).thenReturn(List.of(secretaria));
        ChurchModule override = ChurchModule.builder().churchId(10L).moduleId(1L).enabled(false).build();
        when(churchModuleRepository.findByChurchId(10L)).thenReturn(List.of(override));

        List<ModuleDto> result = newService().getChurchModules(10L);

        assertFalse(result.get(0).isEnabled());
    }

    @Test
    void getChurchModules_unknownChurch_throws() {
        when(churchRepository.existsById(999L)).thenReturn(false);
        ModuleService service = newService();
        assertThrows(ResourceNotFoundException.class, () -> service.getChurchModules(999L));
    }

    @Test
    void getEnabledModuleKeys_withoutChurchId_returnsAllActiveKeys() {
        Module secretaria = Module.builder().id(1L).key("SECRETARIA").name("Secretaria").active(true).build();
        Module tesouraria = Module.builder().id(2L).key("TESOURARIA").name("Tesouraria").active(true).build();
        when(moduleRepository.findByActiveTrueOrderByName()).thenReturn(List.of(secretaria, tesouraria));

        List<String> keys = newService().getEnabledModuleKeys(null);

        assertEquals(List.of("SECRETARIA", "TESOURARIA"), keys);
        verifyNoInteractions(churchRepository);
    }

    @Test
    void setChurchModule_disablingThenReenabling_upsertsSameRow() {
        when(churchRepository.existsById(10L)).thenReturn(true);
        when(moduleRepository.existsById(1L)).thenReturn(true);
        when(churchModuleRepository.findByChurchIdAndModuleId(10L, 1L)).thenReturn(Optional.empty());
        when(churchModuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newService().setChurchModule(10L, 1L, false);

        verify(churchModuleRepository).save(argThat(cm -> !cm.isEnabled() && cm.getChurchId().equals(10L) && cm.getModuleId().equals(1L)));
    }
}
