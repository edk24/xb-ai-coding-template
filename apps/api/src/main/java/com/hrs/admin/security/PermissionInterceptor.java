package com.hrs.admin.security;

import com.hrs.admin.exception.BusinessException;
import com.hrs.admin.vo.AdminUserProfile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 接口级鉴权：只校验"已登录"是不够的，必须校验当前账号是否拥有接口声明的权限标识。
 * 超级管理员短路放行（与既有种子数据一致，避免新增权限后超管被锁在门外）。
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequirePermission required = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (required == null) {
            required = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (required == null) {
            return true;
        }
        AdminUserProfile user = AdminContext.get();
        if (user == null) {
            throw new BusinessException("未登录或登录态失效", 401);
        }
        if (Boolean.TRUE.equals(user.getSuperAdmin())) {
            return true;
        }
        List<String> keys = user.getPermissionKeys();
        if (keys != null && keys.contains(required.value())) {
            return true;
        }
        throw new BusinessException("没有该操作权限", 403);
    }
}
