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
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("organizationId", user.getOrganizationId());
        extraClaims.put("email", user.getEmail());
        return generateToken(user, appProperties.getJwt().getAccessExpiration(), extraClaims);
    }

    public String generateRefreshToken(UserPrincipal user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("type", "refresh");
        extraClaims.put("userId", user.getId());
        extraClaims.put("organizationId", user.getOrganizationId());
        extraClaims.put("email", user.getEmail());
        return generateToken(user, appProperties.getJwt().getRefreshExpiration(), extraClaims);
    }

    public String generateToken(UserPrincipal user, long expiration, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusSeconds(expiration);

        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(user.getUsername())
            .claims(extraClaims)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiryDate))
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

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public Duration getRemainingValidity(String token) {
        Duration remaining = Duration.between(Instant.now(), extractExpiration(token).toInstant());
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public Long extractOrganizationId(String token) {
        return extractClaim(token, claims -> claims.get("organizationId", Long.class));
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenValid(String token, UserPrincipal user) {
        final String username = extractUsername(token);
        return (username.equals(user.getUsername())) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean isRefreshToken(String token) {
        String type = extractClaim(token, claims -> claims.get("type", String.class));
        return "refresh".equals(type);
    }

    private SecretKey getSigningKey() {
        String secret = appProperties.getJwt().getSecret();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
