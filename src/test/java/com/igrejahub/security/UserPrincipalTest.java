package com.igrejahub.security;

import com.igrejahub.permissions.entity.Permission;
import com.igrejahub.roles.entity.Role;
import com.igrejahub.users.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressão: o construtor UserPrincipal(User, mergedPermissions) descartava
 * o parâmetro mergedPermissions (delegava para this(user), que recalcula as
 * permissões só a partir das roles). Isso fazia com que permissões
 * individuais (tabela user_permissions) nunca tivessem efeito real na
 * autorização, apesar de a tela de "Permissões" salvar corretamente.
 */
class UserPrincipalTest {

    private User userWithRole(String roleName, String... rolePermissionNames) {
        Role role = Role.builder().id(1L).name(roleName).active(true).build();
        for (String p : rolePermissionNames) {
            role.getPermissions().add(Permission.builder().id((long) p.hashCode()).name(p).active(true).build());
        }
        User user = new User();
        user.setId(1L);
        user.setName("Usuário Teste");
        user.setEmail("teste@example.com");
        user.setPasswordHash("hash");
        user.setOrganizationId(1L);
        user.setChurchId(10L);
        user.getRoles().add(role);
        return user;
    }

    @Test
    void mergedPermissions_areActuallyApplied() {
        User user = userWithRole("USUARIO", "MEMBER_VIEW");
        Set<String> merged = Set.of("MEMBER_VIEW", "FINANCE_VIEW", "FINANCE_CREATE");

        UserPrincipal principal = new UserPrincipal(user, merged);

        assertTrue(principal.hasPermission("FINANCE_VIEW"),
            "Permissão individual FINANCE_VIEW deveria estar presente após o merge");
        assertTrue(principal.hasPermission("FINANCE_CREATE"),
            "Permissão individual FINANCE_CREATE deveria estar presente após o merge");
    }

    @Test
    void withoutMergedPermissions_fallsBackToRoleOnly() {
        User user = userWithRole("TESOUREIRO", "FINANCE_VIEW");

        UserPrincipal principal = new UserPrincipal(user);

        assertTrue(principal.hasPermission("FINANCE_VIEW"));
        assertFalse(principal.hasPermission("ACCOUNTING_VIEW"));
    }

    @Test
    void mergedPermissions_doNotLoseRoleBasedAuthorities() {
        User user = userWithRole("ADMIN", "USER_CREATE");
        UserPrincipal principal = new UserPrincipal(user, Set.of("USER_CREATE", "FINANCE_VIEW"));

        assertTrue(principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
}
