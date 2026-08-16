package com.igrejahub.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;

    // 10 tentativas de login em 5 minutos antes de bloquear
    private static final int MAX_LOGIN_ATTEMPTS = 10;
    private static final int WINDOW_MINUTES = 5;
    private static final int BLOCK_DURATION_MINUTES = 15;

    public boolean tryLoginRequest(String clientIp) {
        String blockKey = "login_blocked:" + clientIp;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blockKey))) {
            return false;
        }

        String countKey = "login_attempts:" + clientIp;
        String current = redisTemplate.opsForValue().get(countKey);

        if (current == null) {
            redisTemplate.opsForValue().set(countKey, "1", Duration.ofMinutes(WINDOW_MINUTES));
            return true;
        }

        int count = Integer.parseInt(current);
        if (count >= MAX_LOGIN_ATTEMPTS) {
            redisTemplate.opsForValue().set(
                blockKey, "blocked", Duration.ofMinutes(BLOCK_DURATION_MINUTES));
            log.warn("IP {} bloqueado por {} minutos após {} tentativas de login",
                clientIp, BLOCK_DURATION_MINUTES, count);
            return false;
        }

        redisTemplate.opsForValue().increment(countKey);
        return true;
    }

    // Mantido para compatibilidade com AuthenticationService
    public boolean tryRequest(String clientIp) {
        return tryLoginRequest(clientIp);
    }

    public boolean isBlocked(String clientIp) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("login_blocked:" + clientIp));
    }

    public void resetAttempts(String clientIp) {
        redisTemplate.delete("login_attempts:" + clientIp);
        redisTemplate.delete("login_blocked:" + clientIp);
        redisTemplate.delete("rate_limit:" + clientIp);
        redisTemplate.delete("blocked:" + clientIp);
    }
}