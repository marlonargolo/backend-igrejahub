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
    
    private static final int MAX_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_MINUTES = 15;
    private static final int WINDOW_MINUTES = 1;

    public boolean tryRequest(String clientIp) {
        String key = "rate_limit:" + clientIp;
        String blockKey = "blocked:" + clientIp;

        // Verificar se está bloqueado
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blockKey))) {
            return false;
        }

        String currentCount = redisTemplate.opsForValue().get(key);
        
        if (currentCount == null) {
            // Primeira tentativa
            redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(WINDOW_MINUTES));
            return true;
        }

        int count = Integer.parseInt(currentCount);
        if (count >= MAX_ATTEMPTS) {
            // Bloquear o IP
            blockClient(clientIp);
            return false;
        }

        // Incrementar contador
        redisTemplate.opsForValue().increment(key);
        return true;
    }

    public boolean isBlocked(String clientIp) {
        String blockKey = "blocked:" + clientIp;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blockKey));
    }

    private void blockClient(String clientIp) {
        String blockKey = "blocked:" + clientIp;
        redisTemplate.opsForValue().set(
            blockKey, 
            "blocked", 
            Duration.ofMinutes(BLOCK_DURATION_MINUTES)
        );
        log.warn("IP {} bloqueado por {} minutos", clientIp, BLOCK_DURATION_MINUTES);
    }

    public void resetAttempts(String clientIp) {
        String key = "rate_limit:" + clientIp;
        redisTemplate.delete(key);
        String blockKey = "blocked:" + clientIp;
        redisTemplate.delete(blockKey);
    }
}
