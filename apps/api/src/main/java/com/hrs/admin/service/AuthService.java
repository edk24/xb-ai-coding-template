package com.hrs.admin.service;

import com.hrs.admin.common.JwtTokenService;
import com.hrs.admin.common.PasswordUtils;
import com.hrs.admin.dto.AuthRequests;
import com.hrs.admin.exception.BusinessException;
import com.hrs.admin.vo.AdminUserProfile;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final JdbcTemplate jdbc;
    private final JwtTokenService jwtTokenService;
    private final LogService logService;

    public AuthService(JdbcTemplate jdbc, JwtTokenService jwtTokenService, LogService logService) {
        this.jdbc = jdbc;
        this.jwtTokenService = jwtTokenService;
        this.logService = logService;
    }

    @Transactional
    public Map<String, Object> login(AuthRequests.Login request, HttpServletRequest httpRequest) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM admin_users WHERE username = ?", request.getUsername());
        if (rows.isEmpty()) {
            logService.login(null, request.getUsername(), 0, "账号或密码错误", httpRequest);
            throw new BusinessException("账号或密码错误", 200);
        }
        Map<String, Object> user = rows.getFirst();
        Long userId = Rows.lng(user, "id");
        String expected = PasswordUtils.adminPasswordHash(request.getPassword(), Rows.str(user, "salt"));
        if (!expected.equals(Rows.str(user, "password_hash"))) {
            logService.login(userId, request.getUsername(), 0, "账号或密码错误", httpRequest);
            throw new BusinessException("账号或密码错误", 200);
        }
        if (Rows.integer(user, "status") != 1) {
            logService.login(userId, request.getUsername(), 0, "账号已禁用", httpRequest);
            throw new BusinessException("账号已禁用", 403);
        }
        jdbc.update("UPDATE admin_users SET last_login_at = NOW(), last_login_ip = ? WHERE id = ?", logService.ip(httpRequest), userId);
        logService.login(userId, request.getUsername(), 1, "", httpRequest);
        AdminUserProfile profile = profileById(userId);
        String token = jwtTokenService.createToken(profile.getId(), profile.getUsername(), Boolean.TRUE.equals(profile.getSuperAdmin()));
        return Map.of("token", token, "expire_in", jwtTokenService.expireSeconds(), "user", profile);
    }

    public AdminUserProfile profileById(Long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT u.*, d.name AS department_name
            FROM admin_users u
            LEFT JOIN departments d ON d.id = u.department_id
            WHERE u.id = ?
            """, userId);
        if (rows.isEmpty()) {
            throw new BusinessException("用户不存在", 404);
        }
        Map<String, Object> row = rows.getFirst();
        AdminUserProfile profile = new AdminUserProfile();
        profile.setId(Rows.lng(row, "id"));
        profile.setDepartmentId(Rows.lng(row, "department_id"));
        profile.setUsername(Rows.str(row, "username"));
        profile.setNickname(Rows.str(row, "nickname"));
        profile.setAvatar(Rows.str(row, "avatar"));
        profile.setPhone(Rows.str(row, "phone"));
        profile.setEmail(Rows.str(row, "email"));
        profile.setRemark(Rows.str(row, "remark"));
        profile.setStatus(Rows.integer(row, "status"));
        profile.setSuperAdmin(Rows.integer(row, "is_super") == 1);
        profile.setDepartmentName(Rows.str(row, "department_name"));
        profile.setRoles(jdbc.queryForList("""
            SELECT r.name FROM admin_user_roles aur
            JOIN roles r ON r.id = aur.role_id
            WHERE aur.admin_user_id = ?
            ORDER BY r.id
            """, String.class, userId));
        if (Boolean.TRUE.equals(profile.getSuperAdmin())) {
            profile.setPermissionKeys(jdbc.queryForList("SELECT permission_key FROM permissions ORDER BY id", String.class));
        } else {
            profile.setPermissionKeys(jdbc.queryForList("""
                SELECT DISTINCT p.permission_key FROM permissions p
                JOIN role_permissions rp ON rp.permission_id = p.id
                JOIN admin_user_roles aur ON aur.role_id = rp.role_id
                WHERE aur.admin_user_id = ?
                ORDER BY p.id
                """, String.class, userId));
        }
        return profile;
    }

    @Transactional
    public AdminUserProfile updateProfile(Long userId, AuthRequests.Profile request, HttpServletRequest httpRequest) {
        jdbc.update("""
            UPDATE admin_users SET nickname = ?, avatar = ?, phone = ?, email = ?, remark = ?, updated_at = NOW(), updated_by = ?
            WHERE id = ?
            """, request.getNickname(), nz(request.getAvatar()), nz(request.getPhone()), nz(request.getEmail()),
            nz(request.getRemark()), userId, userId);
        logService.operation(userId, "个人资料", "更新资料", httpRequest, Map.of("id", userId), Map.of("success", true));
        return profileById(userId);
    }

    @Transactional
    public void updatePassword(Long userId, AuthRequests.Password request, HttpServletRequest httpRequest) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        Map<String, Object> user = jdbc.queryForMap("SELECT * FROM admin_users WHERE id = ?", userId);
        String oldHash = PasswordUtils.adminPasswordHash(request.getOldPassword(), Rows.str(user, "salt"));
        if (!oldHash.equals(Rows.str(user, "password_hash"))) {
            throw new BusinessException("原密码错误");
        }
        String salt = PasswordUtils.randomSalt();
        String newHash = PasswordUtils.adminPasswordHash(request.getNewPassword(), salt);
        jdbc.update("UPDATE admin_users SET password_hash = ?, salt = ?, updated_at = NOW(), updated_by = ? WHERE id = ?",
            newHash, salt, userId, userId);
        logService.operation(userId, "个人资料", "修改密码", httpRequest, Map.of("id", userId), Map.of("success", true));
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }
}
