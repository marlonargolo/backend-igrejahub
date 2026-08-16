package com.igrejahub.auth.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.users.entity.User;
import com.igrejahub.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String TOKEN_PREFIX = "email_verify:";
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public void sendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", 0L));

        String token = generateVerificationToken();
        redisTemplate.opsForValue().set(TOKEN_PREFIX + token, user.getEmail(), TOKEN_TTL);
        user.setVerified(false);
        userRepository.save(user);

        log.info("🔐 Token de verificação para {}: {}", user.getEmail(), token);
        log.info("📧 Link de verificação: http://localhost:3000/verify-email?token={}", token);
    }

    @Transactional
    public void verifyEmail(String token) {
        String key = TOKEN_PREFIX + token;
        String email = redisTemplate.opsForValue().get(key);
        if (email == null) {
            throw new BusinessException("Token inválido ou expirado");
        }

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
        user.setVerified(true);
        userRepository.save(user);
        redisTemplate.delete(key);
        log.info("✅ Email verificado com sucesso para: {}", user.getEmail());
    }

    private String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }
}
