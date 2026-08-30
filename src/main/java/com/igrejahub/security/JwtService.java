package com.igrejahub.security;

import com.igrejahub.config.ApplicationProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final ApplicationProperties appProperties;

    public String generateAccessToken(UserPrincipal user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId",         user.getId());
        claims.put("organizationId", user.getOrganizationId());
        claims.put("churchId",       user.getChurchId());        // escopo de Igreja
        claims.put("congregationId", user.getCongregationId());  // escopo de Congregação
        claims.put("email",          user.getEmail());
        return generateToken(user, appProperties.getJwt().getAccessExpiration(), claims);
    }

    public String generateRefreshToken(UserPrincipal user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type",           "refresh");
        claims.put("userId",         user.getId());
        claims.put("organizationId", user.getOrganizationId());
        claims.put("churchId",       user.getChurchId());
        claims.put("congregationId", user.getCongregationId());
        claims.put("email",          user.getEmail());
        return generateToken(user, appProperties.getJwt().getRefreshExpiration(), claims);
    }

    public String generateToken(UserPrincipal user, long expiration, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(user.getUsername())
            .claims(extraClaims)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expiration)))
            .signWith(getSigningKey())
            .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, c -> c.get("userId", Long.class));
    }

    public Long extractOrganizationId(String token) {
        return extractClaim(token, c -> c.get("organizationId", Long.class));
    }

    public Long extractChurchId(String token) {
        return extractClaim(token, c -> c.get("churchId", Long.class));
    }

    public Long extractCongregationId(String token) {
        return extractClaim(token, c -> c.get("congregationId", Long.class));
    }

    public String extractEmail(String token) {
        return extractClaim(token, c -> c.get("email", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Duration getRemainingValidity(String token) {
        Duration remaining = Duration.between(Instant.now(), extractExpiration(token).toInstant());
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public boolean isTokenValid(String token, UserPrincipal user) {
        return extractUsername(token).equals(user.getUsername()) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractClaim(token, c -> c.get("type", String.class)));
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
            appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }
}