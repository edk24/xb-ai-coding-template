package com.hrs.admin.controller.admin;

import com.hrs.admin.common.ApiResponse;
import com.hrs.admin.dto.RoleRequest;
import com.hrs.admin.security.AdminContext;
import com.hrs.admin.service.RoleService;
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
@RequestMapping("/admin-api/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody RoleRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(roleService.create(request, AdminContext.get().getId(), httpRequest), "角色已创建");
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody RoleRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(roleService.update(id, request, AdminContext.get().getId(), httpRequest), "角色已更新");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        roleService.delete(id, AdminContext.get().getId(), httpRequest);
        return ApiResponse.ok(null, "角色已删除");
    }
}
