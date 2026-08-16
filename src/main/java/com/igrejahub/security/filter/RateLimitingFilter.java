package com.igrejahub.security.filter;

import com.igrejahub.config.ApplicationProperties;
import com.igrejahub.security.service.RateLimitingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final ApplicationProperties appProperties;

    // Apenas estes endpoints sofrem rate limiting — todos os outros são livres
    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
        "/api/auth/login",
        "/api/auth/register",
        "/auth/login",
        "/auth/register"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (RATE_LIMITED_PATHS.contains(path)) {
            String clientIp = getClientIp(request);

            if (rateLimitingService.isBlocked(clientIp)) {
                log.warn("IP bloqueado tentando acessar {}: {}", path, clientIp);
                writeError(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas de login. Aguarde 15 minutos e tente novamente.");
                return;
            }

            boolean allowed = rateLimitingService.tryLoginRequest(clientIp);
            if (!allowed) {
                log.warn("Rate limit de login excedido para IP: {}", clientIp);
                writeError(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas de login. Aguarde 15 minutos e tente novamente.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            "{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"" + message + "\"}"
        );
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!appProperties.getTrustedProxies().contains(remoteAddr)) {
            return remoteAddr;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return remoteAddr;
        }
        return ip.split(",")[0].trim();
    }
}