package com.igrejahub.security.filter;

import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * "Entrar numa Congregação": um admin de Igreja (sem congregationId fixo) pode
 * enviar o header X-Congregation-Id para navegar temporariamente dentro do
 * escopo de uma Congregação específica — mesmo mecanismo do ROOT com
 * X-Church-Id, um nível abaixo. Precisa ser sempre validado contra a própria
 * Igreja, e nunca pode sobrescrever um usuário já fixo numa Congregação.
 */
@ExtendWith(MockitoExtension.class)
class TenantIsolationFilterCongregationTest {

    @Mock private CongregationRepository congregationRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void authenticateAs(UserPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private UserPrincipal churchAdmin(Long orgId, Long churchId) {
        return new UserPrincipal(1L, "Admin", "admin@a.com", "hash",
            orgId, churchId, null, new HashSet<>(), Set.of("USER_VIEW"));
    }

    private UserPrincipal congregationUser(Long orgId, Long churchId, Long congId) {
        return new UserPrincipal(2L, "Tesoureiro", "tes@a.com", "hash",
            orgId, churchId, congId, new HashSet<>(), Set.of("FINANCE_VIEW"));
    }

    private UserPrincipal root(Long orgId) {
        return new UserPrincipal(3L, "Root", "root@a.com", "hash",
            orgId, null, null, new HashSet<>(), Set.of("ROOT_ACCESS"));
    }

    @Test
    void churchAdmin_entersOwnChurchCongregation_viaHeader() throws Exception {
        authenticateAs(churchAdmin(1L, 10L));
        when(request.getRequestURI()).thenReturn("/dashboard");
        when(request.getHeader("X-Congregation-Id")).thenReturn("100");
        when(congregationRepository.existsByOrganizationIdAndIdAndChurchId(1L, 100L, 10L)).thenReturn(true);

        Long[] captured = new Long[1];
        org.mockito.Mockito.doAnswer(inv -> {
            captured[0] = TenantContext.getCurrentCongregationId();
            return null;
        }).when(filterChain).doFilter(request, response);

        new TenantIsolationFilter(congregationRepository).doFilter(request, response, filterChain);

        assertEquals(100L, captured[0]);
    }

    @Test
    void churchAdmin_cannotEnterCongregationFromAnotherChurch() throws Exception {
        authenticateAs(churchAdmin(1L, 10L));
        when(request.getRequestURI()).thenReturn("/dashboard");
        when(request.getHeader("X-Congregation-Id")).thenReturn("999");
        when(congregationRepository.existsByOrganizationIdAndIdAndChurchId(1L, 999L, 10L)).thenReturn(false);

        Long[] captured = new Long[1];
        org.mockito.Mockito.doAnswer(inv -> {
            captured[0] = TenantContext.getCurrentCongregationId();
            return null;
        }).when(filterChain).doFilter(request, response);

        new TenantIsolationFilter(congregationRepository).doFilter(request, response, filterChain);

        assertNull(captured[0], "congregação de outra Igreja nunca é aplicada");
    }

    @Test
    void root_entersCongregationOfManagedChurch_viaHeader() throws Exception {
        authenticateAs(root(1L));
        when(request.getRequestURI()).thenReturn("/dashboard");
        when(request.getHeader("X-Root-Mode")).thenReturn("filtered");
        when(request.getHeader("X-Church-Id")).thenReturn("10");
        when(request.getHeader("X-Congregation-Id")).thenReturn("100");
        when(congregationRepository.existsByOrganizationIdAndIdAndChurchId(1L, 100L, 10L)).thenReturn(true);

        Long[] capturedChurch = new Long[1];
        Long[] capturedCong = new Long[1];
        org.mockito.Mockito.doAnswer(inv -> {
            capturedChurch[0] = TenantContext.getCurrentChurchId();
            capturedCong[0] = TenantContext.getCurrentCongregationId();
            return null;
        }).when(filterChain).doFilter(request, response);

        new TenantIsolationFilter(congregationRepository).doFilter(request, response, filterChain);

        assertEquals(10L, capturedChurch[0]);
        assertEquals(100L, capturedCong[0],
            "ROOT dentro de uma Igreja precisa também poder filtrar por Congregação — "
            + "sem isso, os módulos devolviam os dados da Igreja inteira dentro da Congregação");
    }

    @Test
    void root_cannotEnterCongregationFromAnotherManagedChurch() throws Exception {
        authenticateAs(root(1L));
        when(request.getRequestURI()).thenReturn("/dashboard");
        when(request.getHeader("X-Root-Mode")).thenReturn("filtered");
        when(request.getHeader("X-Church-Id")).thenReturn("10");
        when(request.getHeader("X-Congregation-Id")).thenReturn("999");
        when(congregationRepository.existsByOrganizationIdAndIdAndChurchId(1L, 999L, 10L)).thenReturn(false);

        Long[] captured = new Long[1];
        org.mockito.Mockito.doAnswer(inv -> {
            captured[0] = TenantContext.getCurrentCongregationId();
            return null;
        }).when(filterChain).doFilter(request, response);

        new TenantIsolationFilter(congregationRepository).doFilter(request, response, filterChain);

        assertNull(captured[0], "congregação de outra Igreja nunca é aplicada, nem para ROOT");
    }

    @Test
    void root_globalMode_neverAppliesCongregationHeader() throws Exception {
        authenticateAs(root(1L));
        when(request.getRequestURI()).thenReturn("/dashboard");
        when(request.getHeader("X-Root-Mode")).thenReturn("global");
        when(request.getHeader("X-Church-Id")).thenReturn(null);
        when(request.getHeader("X-Congregation-Id")).thenReturn("100");

        Long[] capturedChurch = new Long[1];
        Long[] capturedCong = new Long[1];
        org.mockito.Mockito.doAnswer(inv -> {
            capturedChurch[0] = TenantContext.getCurrentChurchId();
            capturedCong[0] = TenantContext.getCurrentCongregationId();
            return null;
        }).when(filterChain).doFilter(request, response);

        new TenantIsolationFilter(congregationRepository).doFilter(request, response, filterChain);

        assertNull(capturedChurch[0]);
        assertNull(capturedCong[0], "sem Igreja selecionada, ROOT nunca herda uma Congregação do header");
    }

    @Test
    void congregationScopedUser_headerNeverOverridesOwnCongregation() throws Exception {
        authenticateAs(congregationUser(1L, 10L, 100L));
        when(request.getRequestURI()).thenReturn("/dashboard");
        // Nem chega a olhar o header: usuário fixo numa congregação usa sempre a do JWT.

        Long[] captured = new Long[1];
        org.mockito.Mockito.doAnswer(inv -> {
            captured[0] = TenantContext.getCurrentCongregationId();
            return null;
        }).when(filterChain).doFilter(request, response);

        new TenantIsolationFilter(congregationRepository).doFilter(request, response, filterChain);

        assertEquals(100L, captured[0], "usuário fixo numa congregação nunca pode ser sobrescrito por header");
    }
}
