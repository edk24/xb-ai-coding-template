package com.hrs.admin.config;

import com.hrs.admin.common.JwtTokenService;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AdminApiProperties.class)
public class AppConfig implements WebMvcConfigurer {
    private final AdminApiProperties properties;

    public AppConfig(AdminApiProperties properties) {
        this.properties = properties;
    }

    @Bean
    public JwtTokenService jwtTokenService(AdminApiProperties properties) {
        return new JwtTokenService(properties.getJwtSecret(), properties.getJwtExpireSeconds());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("*")
            .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Path.of(properties.getUploadRoot()).toAbsolutePath().normalize();
        registry.addResourceHandler(properties.getUploadUrlPrefix() + "/**")
            .addResourceLocations(uploadPath.toUri().toString() + "/");
    }
}
