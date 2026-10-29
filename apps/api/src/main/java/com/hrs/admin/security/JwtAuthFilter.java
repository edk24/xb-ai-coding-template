package com.hrs.admin.security;

import com.hrs.admin.common.CurrentAdminUser;
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
        // CORS 预检不带凭证，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = resolveToken(request);
        if (token == null) {
            throw new BusinessException("未登录或登录态失效", 401);
        }
        CurrentAdminUser tokenUser = parse(token);
        var user = authService.profileById(tokenUser.id());
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已禁用", 403);
        }
        int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        if (tokenUser.tokenVersion() != currentVersion) {
            throw new BusinessException("登录状态已失效，请重新登录", 401);
        }
        if (Boolean.TRUE.equals(user.getMustChangePassword()) && !isPasswordChangeRequest(request)) {
            throw new BusinessException("请先修改初始密码", 403);
        }
        AdminContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminContext.clear();
    }

    /**
     * token 优先取 Authorization: Bearer，其次取 HttpOnly 会话 cookie。
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String value = header.substring(7).trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return authService.tokenFromCookie(request);
    }

    /**
     * 强制改密时只放行查看/修改资料、修改密码与退出登录。
     */
    private boolean isPasswordChangeRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("/admin-api/auth/profile".equals(path)) {
            return "GET".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method);
        }
        if ("/admin-api/auth/password".equals(path)) {
            return "PUT".equalsIgnoreCase(method);
        }
        if ("/admin-api/auth/logout".equals(path)) {
            return "POST".equalsIgnoreCase(method);
        }
        return false;
    }

    private CurrentAdminUser parse(String token) {
        try {
            return jwtTokenService.parseToken(token);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("未登录或登录态失效", 401);
        }
    }
}
