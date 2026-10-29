package com.hrs.admin.config;

import com.hrs.admin.common.JwtTokenService;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AdminApiProperties.class)
public class AppConfig implements WebMvcConfigurer {
    private static final int MIN_JWT_SECRET_LENGTH = 32;
    // 仓库里公开的示例密钥，绝不能用于任何真实环境
    private static final Set<String> INSECURE_JWT_SECRETS = Set.of(
        "change-me-admin-system-secret",
        "local-dev-admin-system-secret-change-me");

    private final AdminApiProperties properties;

    public AppConfig(AdminApiProperties properties) {
        this.properties = properties;
    }

    @Bean
    public JwtTokenService jwtTokenService(AdminApiProperties properties) {
        String secret = properties.getJwtSecret();
        if (secret == null || secret.length() < MIN_JWT_SECRET_LENGTH || INSECURE_JWT_SECRETS.contains(secret)) {
            throw new IllegalStateException("JWT_SECRET 不安全：至少需要 " + MIN_JWT_SECRET_LENGTH
                + " 个字符且不能使用仓库默认示例值，请通过环境变量 JWT_SECRET 配置随机密钥（如 openssl rand -hex 32）");
        }
        return new JwtTokenService(secret, properties.getJwtExpireSeconds());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = Arrays.stream(properties.getCorsAllowedOrigins().split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList();
        registry.addMapping("/**")
            .allowedOriginPatterns(origins.toArray(String[]::new))
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type")
            .allowCredentials(true);
    }
}
