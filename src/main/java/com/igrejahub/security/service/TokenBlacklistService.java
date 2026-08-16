package com.igrejahub.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** Revoga tokens JWT individuais (por jti) antes da expiração natural — usado no logout e na rotação de refresh token. */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "jwt_blacklist:";

    private final RedisTemplate<String, String> redisTemplate;

    public void revoke(String tokenId, Duration ttl) {
        if (tokenId == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redisTemplate.opsForValue().set(PREFIX + tokenId, "1", ttl);
    }

    public boolean isRevoked(String tokenId) {
        return tokenId != null && Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + tokenId));
    }
}
