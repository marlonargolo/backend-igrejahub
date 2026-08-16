package com.igrejahub.auth.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.users.entity.User;
import com.igrejahub.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String TOKEN_PREFIX = "password_reset:";
    private static final Duration TOKEN_TTL = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public void sendResetPasswordEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException("Email não encontrado"));

        String token = generateResetToken();
        redisTemplate.opsForValue().set(TOKEN_PREFIX + token, user.getEmail(), TOKEN_TTL);

        // Em ambiente de produção, enviar email real
        // Por enquanto, apenas loga o token
        log.info("🔐 Token de reset para {}: {}", user.getEmail(), token);
        log.info("📧 Link de reset: http://localhost:3000/reset-password?token={}", token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String key = TOKEN_PREFIX + token;
        String email = redisTemplate.opsForValue().get(key);
        if (email == null) {
            throw new BusinessException("Token inválido ou expirado");
        }

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", 0L));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redisTemplate.delete(key);
        log.info("✅ Senha resetada com sucesso para: {}", email);
    }

    private String generateResetToken() {
        return UUID.randomUUID().toString();
    }
}
