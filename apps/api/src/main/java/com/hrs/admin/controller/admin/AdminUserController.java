package com.hrs.admin.controller.admin;

import com.hrs.admin.common.ApiResponse;
import com.hrs.admin.dto.AdminUserRequest;
import com.hrs.admin.security.AdminContext;
import com.hrs.admin.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
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
    public ApiResponse<?> create(@Valid @RequestBody AdminUserRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(adminUserService.create(request, AdminContext.get().getId(), httpRequest), "管理员已创建");
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody AdminUserRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(adminUserService.update(id, request, AdminContext.get().getId(), httpRequest), "管理员已更新");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        adminUserService.delete(id, AdminContext.get().getId(), httpRequest);
        return ApiResponse.ok(null, "管理员已删除");
    }

    @PutMapping("/{id}/reset-password")
    public ApiResponse<Map<String, String>> resetPassword(@PathVariable Long id, @RequestBody(required = false) Map<String, String> request,
                                                          HttpServletRequest httpRequest) {
        String password = request == null ? null : request.get("new_password");
        return ApiResponse.ok(adminUserService.resetPassword(id, password, AdminContext.get().getId(), httpRequest), "密码已重置");
    }
}
