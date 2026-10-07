package com.igrejahub.users.service;

import com.igrejahub.audit.service.AuditLogService;
import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.roles.entity.Role;
import com.igrejahub.roles.repository.RoleRepository;
import com.igrejahub.security.SecurityUtils;
import com.igrejahub.security.UserPrincipal;
import com.igrejahub.users.dto.CreateUserRequest;
import com.igrejahub.users.entity.User;
import com.igrejahub.users.mapper.UserMapper;
import com.igrejahub.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Regressão: usuários vinculados a uma congregação nunca eram criados de
 * verdade (CreateUserRequest não tinha congregationId; o valor gravado vinha
 * apenas da congregação do próprio criador). Cobre também o isolamento entre
 * congregações da mesma Igreja nas operações de gestão de usuário.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceIsolationTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private AuditLogService auditLogService;
    @Mock private ChurchRepository churchRepository;
    @Mock private CongregationRepository congregationRepository;

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private UserService newService() {
        return new UserService(userRepository, roleRepository, passwordEncoder, userMapper,
            securityUtils, jdbcTemplate, auditLogService, churchRepository, congregationRepository);
    }

    private CreateUserRequest baseRequest(Long churchId, Long congregationId) {
        return CreateUserRequest.builder()
            .name("Fulano").email("fulano@example.com").password("senha1234")
            .churchId(churchId).congregationId(congregationId)
            .build();
    }

    @Test
    void churchAdmin_canAssignNewUserToOwnChurchCongregation() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(null); // admin de Igreja, sem congregação própria
        when(securityUtils.isRoot()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(congregationRepository.existsByOrganizationIdAndIdAndChurchId(1L, 100L, 10L)).thenReturn(true);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newService().createUser(baseRequest(null, 100L));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getChurchId());
        assertEquals(100L, captor.getValue().getCongregationId());
    }

    @Test
    void churchAdmin_cannotAssignCongregationFromAnotherChurch() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(null);
        when(securityUtils.isRoot()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(congregationRepository.existsByOrganizationIdAndIdAndChurchId(1L, 999L, 10L)).thenReturn(false);

        UserService service = newService();
        assertThrows(BusinessException.class, () -> service.createUser(baseRequest(null, 999L)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void congregationScopedCaller_alwaysCreatesUserInOwnCongregation() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(100L); // ex.: PASTOR_CONGREGACAO
        when(securityUtils.isRoot()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Mesmo tentando forjar outra congregação no corpo, o resultado é sempre a própria.
        newService().createUser(baseRequest(null, 200L));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getChurchId());
        assertEquals(100L, captor.getValue().getCongregationId());
        verifyNoInteractions(congregationRepository);
    }

    @Test
    void rootGlobal_alwaysRejected_mustEnterChurchFirst() {
        // ROOT sem Igreja selecionada (modo global) nunca pode criar usuário
        // direto — precisa entrar numa Igreja primeiro, mesmo informando
        // churchId/congregationId válidos no corpo.
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.isRoot()).thenReturn(true);
        when(securityUtils.canViewAll()).thenReturn(true);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        UserService service = newService();
        assertThrows(BusinessException.class, () -> service.createUser(baseRequest(20L, 500L)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void rootFiltered_afterEnteringChurch_acceptsMatchingCongregation() {
        // ROOT que já entrou numa Igreja (modo filtrado) segue o mesmo
        // caminho de um admin de Igreja comum: churchId vem do contexto.
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.isRoot()).thenReturn(true);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(20L);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(congregationRepository.existsByOrganizationIdAndIdAndChurchId(1L, 500L, 20L)).thenReturn(true);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newService().createUser(baseRequest(null, 500L));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(20L, captor.getValue().getChurchId());
        assertEquals(500L, captor.getValue().getCongregationId());
    }

    @Test
    void congregationScopedCaller_cannotManageUserFromSiblingCongregation() {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(100L);
        User other = new User();
        other.setId(5L);
        other.setOrganizationId(1L);
        other.setChurchId(10L);
        other.setCongregationId(200L); // congregação irmã, mesma Igreja
        when(userRepository.findByOrganizationIdAndId(1L, 5L)).thenReturn(Optional.of(other));
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);

        UserService service = newService();
        assertThrows(BusinessException.class, () -> service.getUser(5L));
    }

    @Test
    void setUserPermissions_cannotGrantPermissionCallerDoesNotHave() {
        TenantContext.setCurrentTenant(1L);
        User target = new User();
        target.setId(5L);
        target.setOrganizationId(1L);
        target.setChurchId(10L);
        when(userRepository.findByOrganizationIdAndId(1L, 5L)).thenReturn(Optional.of(target));
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(securityUtils.isRoot()).thenReturn(false);
        UserPrincipal caller = mock(UserPrincipal.class);
        when(caller.getPermissions()).thenReturn(Set.of("FINANCE_VIEW"));
        when(securityUtils.getCurrentUser()).thenReturn(Optional.of(caller));
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any()))
            .thenReturn(java.util.List.of(1L));

        newService().setUserPermissions(5L, Set.of("FINANCE_VIEW", "ACCOUNTING_VIEW"));

        verify(jdbcTemplate).update(
            eq("INSERT INTO user_permissions(user_id,permission_id,granted_by) VALUES(?,?,?) ON CONFLICT DO NOTHING"),
            eq(5L), eq(1L), any());
        verify(jdbcTemplate, times(1)).update(startsWith("INSERT INTO user_permissions"), any(), any(), any());
    }

    @Test
    void restrictedCaller_withoutResolvedCongregation_cannotCreateUser() {
        // accessMainChurch=false e múltiplas Congregações vinculadas, nenhuma
        // escolhida ainda: TenantIsolationFilter manda congId=null +
        // mainChurchAccessDenied=true — não pode criar usuário "na Igreja toda".
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(null);
        TenantContext.setMainChurchAccessDenied(true);
        when(securityUtils.isRoot()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        UserService service = newService();
        assertThrows(BusinessException.class, () -> service.createUser(baseRequest(null, 100L)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void restrictedCaller_cannotManageUserFromUnlinkedCongregation() {
        // Mesma Igreja, mas a congregação do usuário-alvo não é a que o
        // chamador restrito "entrou" — diferente do admin normal, isso aqui
        // nunca basta (nunca cai no fallback de churchId sozinho).
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(100L);
        TenantContext.setMainChurchAccessDenied(true);
        User other = new User();
        other.setId(5L);
        other.setOrganizationId(1L);
        other.setChurchId(10L);
        other.setCongregationId(200L);
        when(userRepository.findByOrganizationIdAndId(1L, 5L)).thenReturn(Optional.of(other));
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);

        UserService service = newService();
        assertThrows(BusinessException.class, () -> service.getUser(5L));
    }

    @Test
    void createUser_withoutAnyRole_fallsBackToDefaultUsuarioRole() {
        // Nenhum roleIds/roleName no corpo: antes o usuário era criado sem
        // role nenhuma (logava mas não acessava nada) — agora cai no perfil
        // básico "USUARIO".
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(null);
        when(securityUtils.isRoot()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        Role usuario = Role.builder().id(9L).name("USUARIO").permissions(Set.of()).build();
        when(roleRepository.findByName("USUARIO")).thenReturn(Optional.of(usuario));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateUserRequest req = CreateUserRequest.builder()
            .name("Fulano").email("fulano@example.com").password("senha1234").build();
        newService().createUser(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Set.of(usuario), captor.getValue().getRoles());
    }

    @Test
    void resetPassword_withExplicitPassword_returnsNullAndEncodesIt() {
        TenantContext.setCurrentTenant(1L);
        User target = new User();
        target.setId(5L);
        target.setOrganizationId(1L);
        target.setChurchId(10L);
        when(userRepository.findByOrganizationIdAndId(1L, 5L)).thenReturn(Optional.of(target));
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = newService().resetPassword(5L, "novaSenha123");

        assertNull(result);
        assertEquals("hashed", target.getPasswordHash());
    }

    @Test
    void resetPassword_withoutPassword_generatesAndReturnsOne() {
        TenantContext.setCurrentTenant(1L);
        User target = new User();
        target.setId(5L);
        target.setOrganizationId(1L);
        target.setChurchId(10L);
        when(userRepository.findByOrganizationIdAndId(1L, 5L)).thenReturn(Optional.of(target));
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = newService().resetPassword(5L, null);

        assertNotNull(result);
        assertFalse(result.isBlank());
        assertEquals("hashed", target.getPasswordHash());
    }

    @Test
    void setUserChurches_nonRootCannotGrantAccessToOtherChurch() {
        TenantContext.setCurrentTenant(1L);
        User target = new User();
        target.setId(5L);
        target.setOrganizationId(1L);
        target.setChurchId(10L);
        when(userRepository.findByOrganizationIdAndId(1L, 5L)).thenReturn(Optional.of(target));
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);

        UserService service = newService();
        assertThrows(BusinessException.class, () -> service.setUserChurches(5L, Set.of(99L)));
    }
}
