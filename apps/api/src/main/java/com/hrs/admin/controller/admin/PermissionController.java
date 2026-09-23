package com.hrs.admin.controller.admin;

import com.hrs.admin.common.ApiResponse;
import com.hrs.admin.dto.PermissionRequest;
import com.hrs.admin.security.AdminContext;
import com.hrs.admin.service.PermissionService;
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
@RequestMapping("/admin-api/permissions")
public class PermissionController {
    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody PermissionRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(permissionService.create(request, AdminContext.get().getId(), httpRequest), "权限已创建");
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody PermissionRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(permissionService.update(id, request, AdminContext.get().getId(), httpRequest), "权限已更新");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        permissionService.delete(id, AdminContext.get().getId(), httpRequest);
        return ApiResponse.ok(null, "权限已删除");
    }
}
