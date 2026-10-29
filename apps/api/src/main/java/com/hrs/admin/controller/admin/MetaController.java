package com.hrs.admin.controller.admin;

import com.hrs.admin.common.ApiResponse;
import com.hrs.admin.mapper.AdminMapper;
import com.hrs.admin.security.AdminContext;
import com.hrs.admin.security.RequirePermission;
import com.hrs.admin.service.QueryService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api")
public class MetaController {
    private final QueryService queryService;
    private final AdminMapper adminMapper;

    public MetaController(QueryService queryService, AdminMapper adminMapper) {
        this.queryService = queryService;
        this.adminMapper = adminMapper;
    }

    @GetMapping("/meta/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        var user = AdminContext.get();
        return ApiResponse.ok(Map.of(
            "welcome", "欢迎回来，" + user.getNickname(),
            "stats", Map.of(
                "admin_users", adminMapper.countAdminUsers(),
                "roles", adminMapper.countRoles(),
                "departments", adminMapper.countDepartments(),
                "permissions", adminMapper.countPermissions()
            )
        ));
    }

    @GetMapping("/meta/menu-tree")
    public ApiResponse<?> menuTree() {
        var user = AdminContext.get();
        return ApiResponse.ok(queryService.menuTreeForUser(user.getId(), Boolean.TRUE.equals(user.getSuperAdmin())));
    }

    @GetMapping("/admin-users")
    @RequirePermission("admin-users:view")
    public ApiResponse<?> adminUsers() {
        return ApiResponse.ok(queryService.adminUsers());
    }

    @GetMapping("/roles")
    @RequirePermission("roles:view")
    public ApiResponse<?> roles() {
        return ApiResponse.ok(queryService.roles());
    }

    @GetMapping("/departments/tree")
    @RequirePermission("departments:view")
    public ApiResponse<?> departments() {
        return ApiResponse.ok(queryService.departmentTree());
    }

    @GetMapping("/permissions/tree")
    @RequirePermission("permissions:view")
    public ApiResponse<?> permissions() {
        return ApiResponse.ok(queryService.permissionTree());
    }

    @GetMapping("/config/items")
    @RequirePermission("config:view")
    public ApiResponse<?> configItems() {
        return ApiResponse.ok(queryService.configItems());
    }

    @GetMapping("/login-logs")
    @RequirePermission("login-logs:view")
    public ApiResponse<?> loginLogs() {
        return ApiResponse.ok(queryService.loginLogs());
    }

    @GetMapping("/operation-logs")
    @RequirePermission("operation-logs:view")
    public ApiResponse<?> operationLogs() {
        return ApiResponse.ok(queryService.operationLogs());
    }
}
