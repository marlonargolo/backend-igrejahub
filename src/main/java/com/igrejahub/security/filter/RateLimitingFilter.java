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

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final ApplicationProperties appProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        
        // Aplicar rate limiting apenas em endpoints de autenticação
        if (path.startsWith("/auth/") || path.startsWith("/api/auth/")) {
            String clientIp = getClientIp(request);
            
            // Verificar se o IP está bloqueado
            if (rateLimitingService.isBlocked(clientIp)) {
                log.warn("IP bloqueado por excesso de tentativas: {}", clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
                return;
            }
            
            // Tentar registrar a requisição
            boolean allowed = rateLimitingService.tryRequest(clientIp);
            if (!allowed) {
                log.warn("Rate limit excedido para IP: {}", clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("{\"error\": \"Rate limit exceeded. Please try again later.\"}");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Só confia em X-Forwarded-For/etc quando a requisição chega diretamente de um proxy
     * reverso confiável (app.trusted-proxies); caso contrário, o header é forjável pelo
     * próprio cliente e usá-lo permitiria burlar o rate limit trocando o valor a cada tentativa.
     */
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
