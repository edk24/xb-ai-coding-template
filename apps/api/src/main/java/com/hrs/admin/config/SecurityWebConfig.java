package com.hrs.admin.config;

import com.hrs.admin.security.JwtAuthFilter;
import com.hrs.admin.security.PermissionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityWebConfig implements WebMvcConfigurer {
    private final JwtAuthFilter jwtAuthFilter;
    private final PermissionInterceptor permissionInterceptor;

    public SecurityWebConfig(JwtAuthFilter jwtAuthFilter, PermissionInterceptor permissionInterceptor) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 先认证、后鉴权：顺序不能颠倒，否则权限拦截器拿不到当前登录用户。
        registry.addInterceptor(jwtAuthFilter)
            .addPathPatterns("/admin-api/**")
            .excludePathPatterns("/admin-api/auth/login")
            .order(0);
        registry.addInterceptor(permissionInterceptor)
            .addPathPatterns("/admin-api/**")
            .excludePathPatterns("/admin-api/auth/login")
            .order(1);
    }
}
