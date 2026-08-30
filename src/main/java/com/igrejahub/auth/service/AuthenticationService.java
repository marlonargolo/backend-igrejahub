package com.igrejahub.auth.service;

import com.igrejahub.auth.dto.*;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.config.ApplicationProperties;
import com.igrejahub.organizations.entity.Organization;
import com.igrejahub.organizations.repository.OrganizationRepository;
import com.igrejahub.roles.repository.RoleRepository;
import com.igrejahub.security.JwtService;
import com.igrejahub.security.UserPrincipal;
import com.igrejahub.security.service.RateLimitingService;
import com.igrejahub.security.service.TokenBlacklistService;
import com.igrejahub.users.entity.User;
import com.igrejahub.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RateLimitingService rateLimitingService;
    private final TokenBlacklistService tokenBlacklistService;
    private final ApplicationProperties appProperties;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_FAILED_ATTEMPTS  = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    @Transactional
    public LoginResponse login(LoginRequest request, String ip) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
            if (!user.isActive()) throw new BusinessException("Usuário inativo");
            if (user.isLocked()) throw new LockedException("Conta bloqueada temporariamente");

            user.resetFailedAttempts();
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(ip);
            userRepository.save(user);
            rateLimitingService.resetAttempts(ip);

            // Reconstruir UserPrincipal do banco para garantir churchId/congregationId atualizados
            UserPrincipal principalWithScope = new UserPrincipal(user);
            String accessToken  = jwtService.generateAccessToken(principalWithScope);
            String refreshToken = jwtService.generateRefreshToken(principalWithScope);

            return buildLoginResponse(user, accessToken, refreshToken);

        } catch (BadCredentialsException e) {
            handleFailedLogin(request.getEmail(), ip);
            throw new BadCredentialsException("Credenciais inválidas");
        } catch (LockedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Login error: {} — {}", e.getClass().getName(), e.getMessage(), e);
            throw new BusinessException("Erro ao realizar login");
        }
    }

    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String username = jwtService.extractUsername(refreshToken);

        if (username == null || !jwtService.isRefreshToken(refreshToken)
                || tokenBlacklistService.isRevoked(jwtService.extractTokenId(refreshToken))) {
            throw new BusinessException("Token inválido");
        }

        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
        if (!user.isActive()) throw new BusinessException("Usuário inativo");

        UserPrincipal userPrincipal = new UserPrincipal(user);
        String newAccessToken  = jwtService.generateAccessToken(userPrincipal);
        String newRefreshToken = jwtService.generateRefreshToken(userPrincipal);

        tokenBlacklistService.revoke(
            jwtService.extractTokenId(refreshToken),
            jwtService.getRemainingValidity(refreshToken)
        );

        return RefreshTokenResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(appProperties.getJwt().getAccessExpiration())
            .build();
    }

    @Transactional
    public LoginResponse register(RegisterRequest request, String ip) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email já cadastrado: " + request.getEmail(), "EMAIL_ALREADY_EXISTS");
        }

        Organization organization = Organization.builder()
            .name(request.getOrganizationName())
            .email(request.getEmail())
            .cnpj(request.getCnpj())
            .city(request.getCity())
            .state(request.getState())
            .plan("FREE")
            .active(true)
            .build();
        organization.setOrganizationId(0L);
        organization = organizationRepository.save(organization);
        organization.setOrganizationId(organization.getId());
        organization = organizationRepository.save(organization);

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setOrganizationId(organization.getId());
        user.setActive(true);
        user.setVerified(false);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ip);
        roleRepository.findByName("ADMIN").ifPresent(user.getRoles()::add);
        user = userRepository.save(user);

        UserPrincipal userPrincipal = new UserPrincipal(user);
        String accessToken  = jwtService.generateAccessToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);
        return buildLoginResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public void logout(String token) {
        try {
            tokenBlacklistService.revoke(
                jwtService.extractTokenId(token),
                jwtService.getRemainingValidity(token)
            );
        } catch (Exception e) {
            log.debug("Não foi possível revogar token no logout: {}", e.getMessage());
        }
        SecurityContextHolder.clearContext();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void handleFailedLogin(String email, String ip) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.incrementFailedAttempts();
            if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.lockAccount(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                log.warn("User {} locked for {} minutes", email, LOCK_DURATION_MINUTES);
            }
            userRepository.save(user);
        });
    }

    private LoginResponse buildLoginResponse(User user, String accessToken, String refreshToken) {
        Organization organization = organizationRepository.findById(user.getOrganizationId()).orElse(null);

        UserInfoDto userInfo = UserInfoDto.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .organizationId(user.getOrganizationId())
            .organizationName(organization != null ? organization.getName() : null)
            .churchId(user.getChurchId())
            .congregationId(user.getCongregationId())
            .roles(user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet()))
            .permissions(user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getName())
                .collect(Collectors.toSet()))
            .build();

        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(appProperties.getJwt().getAccessExpiration())
            .user(userInfo)
            .build();
    }
}