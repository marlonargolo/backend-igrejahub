package com.igrejahub.auth.controller;

import com.igrejahub.auth.dto.*;
import com.igrejahub.auth.service.AuthenticationService;
import com.igrejahub.auth.service.EmailVerificationService;
import com.igrejahub.common.dto.ApiResponse;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.organizations.entity.Organization;
import com.igrejahub.organizations.repository.OrganizationRepository;
import com.igrejahub.security.UserPrincipal;
import com.igrejahub.users.entity.User;
import com.igrejahub.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints de autenticação")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    @Operation(summary = "Login")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        LoginResponse response = authenticationService.login(request, ip);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authenticationService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Logout", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authenticationService.logout(token);
        }
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "Get current user info", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoDto>> getCurrentUser() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();

        // Busca o User completo para ter name real e roles/permissions atualizados
        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));

        Organization organization = organizationRepository
            .findById(user.getOrganizationId()).orElse(null);

        UserInfoDto dto = UserInfoDto.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .organizationId(user.getOrganizationId())
            .organizationName(organization != null ? organization.getName() : null)
            .roles(user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet()))
            .permissions(user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getName())
                .collect(Collectors.toSet()))
            .build();

        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @Operation(summary = "Registrar nova organização e usuário administrador")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        LoginResponse response = authenticationService.register(request, ip);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Verificar email")
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verifyEmail(request.getToken());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
