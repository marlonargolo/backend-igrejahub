package com.igrejahub.auth.service;

import com.igrejahub.auth.dto.LoginRequest;
import com.igrejahub.auth.dto.LoginResponse;
import com.igrejahub.config.ApplicationProperties;
import com.igrejahub.organizations.repository.OrganizationRepository;
import com.igrejahub.roles.entity.Role;
import com.igrejahub.roles.repository.RoleRepository;
import com.igrejahub.security.JwtService;
import com.igrejahub.security.UserPrincipal;
import com.igrejahub.security.service.CustomUserDetailsService;
import com.igrejahub.security.service.RateLimitingService;
import com.igrejahub.security.service.TokenBlacklistService;
import com.igrejahub.users.entity.User;
import com.igrejahub.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Regressão: a resposta de login (e /auth/me) recalculava as permissões só a
 * partir das roles do usuário, nunca incluindo as permissões individuais
 * (user_permissions). Mesmo com o UserPrincipal corrigido, o frontend
 * continuava recebendo um conjunto de permissões incompleto no login.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceLoginPermissionsTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private RateLimitingService rateLimitingService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CustomUserDetailsService userDetailsService;

    @Test
    void login_usesMergedPermissionsFromUserDetailsService_notRoleOnly() {
        AuthenticationService service = new AuthenticationService(
            authenticationManager, jwtService, userRepository, organizationRepository,
            rateLimitingService, tokenBlacklistService, new ApplicationProperties(),
            roleRepository, passwordEncoder, userDetailsService);

        User user = new User();
        user.setId(1L);
        user.setName("Fulano");
        user.setEmail("fulano@example.com");
        user.setOrganizationId(1L);
        user.setChurchId(10L);
        Role role = Role.builder().id(1L).name("TESOUREIRO").active(true).build();
        user.getRoles().add(role); // role sem FINANCE_CREATE — só via permissão individual

        UserPrincipal loginPrincipal = new UserPrincipal(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            loginPrincipal, null, loginPrincipal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Permissões mescladas (role + individual) que o CustomUserDetailsService devolveria
        UserPrincipal mergedPrincipal = new UserPrincipal(user, Set.of("FINANCE_VIEW", "FINANCE_CREATE"));
        when(userDetailsService.loadUserByUsername("fulano@example.com")).thenReturn(mergedPrincipal);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

        LoginRequest request = new LoginRequest();
        request.setEmail("fulano@example.com");
        request.setPassword("senha1234");

        LoginResponse response = service.login(request, "127.0.0.1");

        assertTrue(response.getUser().getPermissions().contains("FINANCE_CREATE"),
            "A resposta de login deve incluir permissões individuais, não só as da role");
    }
}
