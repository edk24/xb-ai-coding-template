package com.hrs.admin.security;

import com.hrs.admin.common.JwtTokenService;
import com.hrs.admin.exception.BusinessException;
import com.hrs.admin.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthFilter implements HandlerInterceptor {
    private final JwtTokenService jwtTokenService;
    private final AuthService authService;

    public JwtAuthFilter(JwtTokenService jwtTokenService, AuthService authService) {
        this.jwtTokenService = jwtTokenService;
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException("未登录或登录态失效", 401);
        }
        var tokenUser = parse(header.substring(7));
        var user = authService.profileById(tokenUser.id());
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已禁用", 403);
        }
        AdminContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminContext.clear();
    }

    private com.hrs.admin.common.CurrentAdminUser parse(String token) {
        try {
            return jwtTokenService.parseToken(token);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("未登录或登录态失效", 401);
        }
    }
}
