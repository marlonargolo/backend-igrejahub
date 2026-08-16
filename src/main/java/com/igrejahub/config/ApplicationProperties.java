package com.igrejahub.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {
    private static final String DEFAULT_JWT_SECRET =
            "default-secret-key-change-in-production-very-long-secret-key-for-hs512";

    private String frontendUrl;
    private String rootEmail;
    private String rootPassword;
    private List<String> corsAllowedOrigins;
    /** IPs de proxies reversos confiáveis; só nesses casos os headers X-Forwarded-For/etc são aceitos como IP real do cliente. Vazio por padrão (nenhum proxy confiável). */
    private List<String> trustedProxies = new java.util.ArrayList<>();
    private JwtProperties jwt = new JwtProperties();
    private SecurityProperties security = new SecurityProperties();

    // Injetado pelo Spring — não é uma propriedade @ConfigurationProperties
    @org.springframework.beans.factory.annotation.Autowired
    private Environment environment;

    @PostConstruct
    public void validate() {
        if (DEFAULT_JWT_SECRET.equals(jwt.getSecret())) {
            boolean isProd = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
            if (isProd) {
                throw new IllegalStateException(
                    "ERRO CRÍTICO: JWT_SECRET está com o valor padrão inseguro em ambiente de produção. " +
                    "Configure a variável de ambiente JWT_SECRET com um valor único e secreto (mínimo 64 caracteres).");
            }
            log.warn("!!! JWT_SECRET usando valor padrão — NUNCA use isso em produção. " +
                     "Qualquer pessoa com acesso ao código-fonte pode forjar tokens válidos. !!!");
        }
    }

    @Data
    public static class JwtProperties {
        private String secret = DEFAULT_JWT_SECRET;
        private long accessExpiration = 900;
        private long refreshExpiration = 604800;
    }

    @Data
    public static class SecurityProperties {
        private RateLimitProperties rateLimit = new RateLimitProperties();

        @Data
        public static class RateLimitProperties {
            private int maxAttempts = 5;
            private int blockDuration = 15;
        }
    }
}
