package com.igrejahub.auth.controller;

import com.igrejahub.auth.dto.ForgotPasswordRequest;
import com.igrejahub.auth.dto.ResetPasswordRequest;
import com.igrejahub.auth.service.PasswordResetService;
import com.igrejahub.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints de autenticação")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordResetService passwordResetService;

    @Operation(summary = "Solicitar redefinição de senha")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.sendResetPasswordEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "Redefinir senha")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
