package com.hrs.admin.controller.admin;

import com.hrs.admin.common.ApiResponse;
import com.hrs.admin.dto.DepartmentRequest;
import com.hrs.admin.security.AdminContext;
import com.hrs.admin.service.DepartmentService;
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
@RequestMapping("/admin-api/departments")
public class DepartmentController {
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody DepartmentRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(departmentService.create(request, AdminContext.get().getId(), httpRequest), "部门已创建");
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(departmentService.update(id, request, AdminContext.get().getId(), httpRequest), "部门已更新");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        departmentService.delete(id, AdminContext.get().getId(), httpRequest);
        return ApiResponse.ok(null, "部门已删除");
    }
}
