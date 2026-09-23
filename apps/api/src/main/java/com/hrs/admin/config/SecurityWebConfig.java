package com.hrs.admin.config;

import com.hrs.admin.security.JwtAuthFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityWebConfig implements WebMvcConfigurer {
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityWebConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthFilter)
            .addPathPatterns("/admin-api/**")
            .excludePathPatterns("/admin-api/auth/login");
    }
}
