package com.hrs.admin.controller.admin;

import com.hrs.admin.common.ApiResponse;
import com.hrs.admin.dto.AdminUserRequest;
import com.hrs.admin.security.AdminContext;
import com.hrs.admin.security.RequirePermission;
import com.hrs.admin.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/admin-users")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping
    @RequirePermission("admin-users:create")
    public ApiResponse<?> create(@Valid @RequestBody AdminUserRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(adminUserService.create(request, AdminContext.get().getId(), httpRequest), "管理员已创建");
    }

    @PutMapping("/{id}")
    @RequirePermission("admin-users:update")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody AdminUserRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(adminUserService.update(id, request, AdminContext.get().getId(), httpRequest), "管理员已更新");
    }

    @DeleteMapping("/{id}")
    @RequirePermission("admin-users:delete")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        adminUserService.delete(id, AdminContext.get().getId(), httpRequest);
        return ApiResponse.ok(null, "管理员已删除");
    }

    @PutMapping("/{id}/reset-password")
    @RequirePermission("admin-users:reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminUserRequest.ResetPassword request,
                                           HttpServletRequest httpRequest) {
        adminUserService.resetPassword(id, request.getNewPassword(), AdminContext.get().getId(), httpRequest);
        return ApiResponse.ok(null, "密码已重置");
    }
}
