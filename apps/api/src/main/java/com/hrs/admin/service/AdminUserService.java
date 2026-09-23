package com.hrs.admin.service;

import com.hrs.admin.common.PasswordUtils;
import com.hrs.admin.dto.AdminUserRequest;
import com.hrs.admin.exception.BusinessException;
import com.hrs.admin.vo.AdminUserRow;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    private final JdbcTemplate jdbc;
    private final QueryService queryService;
    private final LogService logService;

    public AdminUserService(JdbcTemplate jdbc, QueryService queryService, LogService logService) {
        this.jdbc = jdbc;
        this.queryService = queryService;
        this.logService = logService;
    }

    @Transactional
    public AdminUserRow create(AdminUserRequest request, Long operatorId, HttpServletRequest httpRequest) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("请输入登录密码");
        }
        assertDepartmentExists(request.getDepartmentId());
        assertUniqueUsername(request.getUsername(), null);
        String salt = PasswordUtils.randomSalt();
        String hash = PasswordUtils.adminPasswordHash(request.getPassword(), salt);
        jdbc.update("""
            INSERT INTO admin_users (department_id, username, nickname, avatar, phone, email, password_hash, salt, status, is_super, remark, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?)
            """, request.getDepartmentId(), request.getUsername(), request.getNickname(), nz(request.getAvatar()),
            nz(request.getPhone()), nz(request.getEmail()), hash, salt, request.getStatus(), nz(request.getRemark()), operatorId, operatorId);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        replaceRoles(id, request);
        logService.operation(operatorId, "管理员管理", "新增管理员", httpRequest, Map.of("username", request.getUsername()), Map.of("id", id));
        return find(id);
    }

    @Transactional
    public AdminUserRow update(Long id, AdminUserRequest request, Long operatorId, HttpServletRequest httpRequest) {
        Map<String, Object> old = requireUser(id);
        AdminUserServiceRules.ensureSuperAdminCanUseStatus(Rows.integer(old, "is_super") == 1, request.getStatus());
        assertDepartmentExists(request.getDepartmentId());
        assertUniqueUsername(request.getUsername(), id);
        jdbc.update("""
            UPDATE admin_users SET department_id = ?, username = ?, nickname = ?, avatar = ?, phone = ?, email = ?,
              status = ?, remark = ?, updated_at = NOW(), updated_by = ?
            WHERE id = ?
            """, request.getDepartmentId(), request.getUsername(), request.getNickname(), nz(request.getAvatar()),
            nz(request.getPhone()), nz(request.getEmail()), request.getStatus(), nz(request.getRemark()), operatorId, id);
        replaceRoles(id, request);
        logService.operation(operatorId, "管理员管理", "编辑管理员", httpRequest, Map.of("id", id), Map.of("success", true));
        return find(id);
    }

    @Transactional
    public void delete(Long id, Long operatorId, HttpServletRequest httpRequest) {
        Map<String, Object> user = requireUser(id);
        AdminUserServiceRules.ensureCanDelete(Rows.integer(user, "is_super") == 1, id.equals(operatorId));
        jdbc.update("DELETE FROM admin_user_roles WHERE admin_user_id = ?", id);
        jdbc.update("DELETE FROM admin_users WHERE id = ?", id);
        logService.operation(operatorId, "管理员管理", "删除管理员", httpRequest, Map.of("id", id), Map.of("success", true));
    }

    @Transactional
    public Map<String, String> resetPassword(Long id, String clientMd5Password, Long operatorId, HttpServletRequest httpRequest) {
        Map<String, Object> user = requireUser(id);
        AdminUserServiceRules.ensureCanResetPassword(Rows.integer(user, "is_super") == 1);
        String password = (clientMd5Password == null || clientMd5Password.isBlank()) ? PasswordUtils.md5Hex("123456") : clientMd5Password;
        String salt = PasswordUtils.randomSalt();
        jdbc.update("UPDATE admin_users SET password_hash = ?, salt = ?, updated_at = NOW(), updated_by = ? WHERE id = ?",
            PasswordUtils.adminPasswordHash(password, salt), salt, operatorId, id);
        logService.operation(operatorId, "管理员管理", "重置密码", httpRequest, Map.of("id", id), Map.of("success", true));
        return Map.of("new_password", clientMd5Password == null || clientMd5Password.isBlank() ? "123456" : clientMd5Password);
    }

    private AdminUserRow find(Long id) {
        return queryService.adminUsers().stream()
            .filter(user -> user.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new BusinessException("管理员不存在", 404));
    }

    private Map<String, Object> requireUser(Long id) {
        var rows = jdbc.queryForList("SELECT * FROM admin_users WHERE id = ?", id);
        if (rows.isEmpty()) {
            throw new BusinessException("管理员不存在", 404);
        }
        return rows.getFirst();
    }

    private void replaceRoles(Long userId, AdminUserRequest request) {
        jdbc.update("DELETE FROM admin_user_roles WHERE admin_user_id = ?", userId);
        if (request.getRoleIds() != null) {
            for (Long roleId : request.getRoleIds()) {
                if (jdbc.queryForObject("SELECT COUNT(*) FROM roles WHERE id = ?", Long.class, roleId) == 0) {
                    throw new BusinessException("角色不存在");
                }
                jdbc.update("INSERT INTO admin_user_roles (admin_user_id, role_id) VALUES (?, ?)", userId, roleId);
            }
        }
    }

    private void assertDepartmentExists(Long departmentId) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM departments WHERE id = ?", Long.class, departmentId) == 0) {
            throw new BusinessException("部门不存在");
        }
    }

    private void assertUniqueUsername(String username, Long ignoreId) {
        Long count = ignoreId == null
            ? jdbc.queryForObject("SELECT COUNT(*) FROM admin_users WHERE username = ?", Long.class, username)
            : jdbc.queryForObject("SELECT COUNT(*) FROM admin_users WHERE username = ? AND id <> ?", Long.class, username, ignoreId);
        if (count != null && count > 0) {
            throw new BusinessException("账号已存在");
        }
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }
}
