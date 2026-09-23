package com.hrs.admin.service;

import com.hrs.admin.dto.RoleRequest;
import com.hrs.admin.exception.BusinessException;
import com.hrs.admin.vo.RoleRow;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {
    private final JdbcTemplate jdbc;
    private final QueryService queryService;
    private final LogService logService;

    public RoleService(JdbcTemplate jdbc, QueryService queryService, LogService logService) {
        this.jdbc = jdbc;
        this.queryService = queryService;
        this.logService = logService;
    }

    @Transactional
    public RoleRow create(RoleRequest request, Long operatorId, HttpServletRequest httpRequest) {
        assertUnique(request.getName(), request.getCode(), null);
        jdbc.update("""
            INSERT INTO roles (name, code, status, data_scope, remark, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """, request.getName(), request.getCode(), request.getStatus(), request.getDataScope(), nz(request.getRemark()), operatorId, operatorId);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        replacePermissions(id, request);
        logService.operation(operatorId, "角色管理", "新增角色", httpRequest, Map.of("name", request.getName()), Map.of("id", id));
        return find(id);
    }

    @Transactional
    public RoleRow update(Long id, RoleRequest request, Long operatorId, HttpServletRequest httpRequest) {
        requireRole(id);
        assertUnique(request.getName(), request.getCode(), id);
        jdbc.update("""
            UPDATE roles SET name = ?, code = ?, status = ?, data_scope = ?, remark = ?, updated_by = ?, updated_at = NOW()
            WHERE id = ?
            """, request.getName(), request.getCode(), request.getStatus(), request.getDataScope(), nz(request.getRemark()), operatorId, id);
        replacePermissions(id, request);
        logService.operation(operatorId, "角色管理", "编辑角色", httpRequest, Map.of("id", id), Map.of("success", true));
        return find(id);
    }

    @Transactional
    public void delete(Long id, Long operatorId, HttpServletRequest httpRequest) {
        requireRole(id);
        if (id == 1) {
            throw new BusinessException("内置角色不允许删除");
        }
        Long used = jdbc.queryForObject("SELECT COUNT(*) FROM admin_user_roles WHERE role_id = ?", Long.class, id);
        if (used != null && used > 0) {
            throw new BusinessException("角色已绑定管理员，不能删除");
        }
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", id);
        jdbc.update("DELETE FROM roles WHERE id = ?", id);
        logService.operation(operatorId, "角色管理", "删除角色", httpRequest, Map.of("id", id), Map.of("success", true));
    }

    private RoleRow find(Long id) {
        return queryService.roles().stream()
            .filter(role -> role.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new BusinessException("角色不存在", 404));
    }

    private void requireRole(Long id) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM roles WHERE id = ?", Long.class, id) == 0) {
            throw new BusinessException("角色不存在", 404);
        }
    }

    private void replacePermissions(Long roleId, RoleRequest request) {
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", roleId);
        if (request.getPermissionIds() == null) {
            return;
        }
        for (Long permissionId : request.getPermissionIds()) {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM permissions WHERE id = ?", Long.class, permissionId) == 0) {
                throw new BusinessException("权限不存在");
            }
            jdbc.update("INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)", roleId, permissionId);
        }
    }

    private void assertUnique(String name, String code, Long ignoreId) {
        Long count = ignoreId == null
            ? jdbc.queryForObject("SELECT COUNT(*) FROM roles WHERE name = ? OR code = ?", Long.class, name, code)
            : jdbc.queryForObject("SELECT COUNT(*) FROM roles WHERE (name = ? OR code = ?) AND id <> ?", Long.class, name, code, ignoreId);
        if (count != null && count > 0) {
            throw new BusinessException("角色名称或编码已存在");
        }
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }
}
