package com.hrs.admin.service;

import com.hrs.admin.common.JwtTokenService;
import com.hrs.admin.common.PasswordUtils;
import com.hrs.admin.dto.AuthRequests;
import com.hrs.admin.exception.BusinessException;
import com.hrs.admin.vo.AdminUserProfile;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    public static final String TOKEN_COOKIE = "ADMIN_TOKEN";

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    // 历史弱默认密码兜底：即使库被恢复回默认密码，也强制走改密流程
    private static final Set<String> WEAK_DEFAULT_PASSWORDS = Set.of("123456", "admin123", "password");

    private final JdbcTemplate jdbc;
    private final JwtTokenService jwtTokenService;
    private final LogService logService;
    private final LoginThrottleService loginThrottleService;

    public AuthService(JdbcTemplate jdbc, JwtTokenService jwtTokenService, LogService logService,
                       LoginThrottleService loginThrottleService) {
        this.jdbc = jdbc;
        this.jwtTokenService = jwtTokenService;
        this.logService = logService;
        this.loginThrottleService = loginThrottleService;
    }

    /**
     * 登录：密码为明文，失败/锁定写 login_logs。注意不要套事务，否则抛异常时失败日志会回滚。
     */
    public Map<String, Object> login(AuthRequests.Login request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String ip = loginThrottleService.clientIp(httpRequest);
        if (loginThrottleService.isLocked(username, ip)) {
            long minutes = loginThrottleService.remainingMinutes(username, ip);
            logService.login(null, username, 0, "登录失败次数过多，账号已锁定", httpRequest);
            log.warn("登录被限流拒绝: username={}, ip={}, 剩余{}分钟", username, ip, minutes);
            throw new BusinessException("登录失败次数过多，请 " + minutes + " 分钟后再试", 429);
        }
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM admin_users WHERE username = ?", username);
        if (rows.isEmpty()) {
            throw credentialError(username, ip, null, httpRequest);
        }
        Map<String, Object> user = rows.getFirst();
        Long userId = Rows.lng(user, "id");
        String salt = Rows.str(user, "salt");
        if (!PasswordUtils.verify(request.getPassword(), Rows.str(user, "password_hash"), salt)) {
            throw credentialError(username, ip, userId, httpRequest);
        }
        if (Rows.integer(user, "status") != 1) {
            logService.login(userId, username, 0, "账号已禁用", httpRequest);
            log.warn("登录失败: username={}, ip={}, 原因=账号已禁用", username, ip);
            throw new BusinessException("账号已禁用", 403);
        }
        loginThrottleService.clear(username, ip);
        upgradeLegacyPasswordIfNeeded(userId, request.getPassword(), salt);
        if (Rows.integer(user, "must_change_password") == 0 && WEAK_DEFAULT_PASSWORDS.contains(request.getPassword())) {
            // 弱默认密码兜底：库被恢复回默认密码时，V3 不会再执行，这里强制改密
            jdbc.update("UPDATE admin_users SET must_change_password = 1, updated_at = NOW() WHERE id = ?", userId);
            log.warn("检测到弱默认密码，已强制要求修改密码: username={}, userId={}", username, userId);
        }
        jdbc.update("UPDATE admin_users SET last_login_at = NOW(), last_login_ip = ? WHERE id = ?", logService.ip(httpRequest), userId);
        logService.login(userId, username, 1, "", httpRequest);
        log.info("登录成功: username={}, ip={}", username, ip);
        AdminUserProfile profile = profileById(userId);
        String token = jwtTokenService.createToken(profile.getId(), profile.getUsername(),
            Boolean.TRUE.equals(profile.getSuperAdmin()), profile.getTokenVersion());
        writeTokenCookie(httpResponse, httpRequest, token);
        return Map.of("token", token, "expire_in", jwtTokenService.expireSeconds(), "user", profile);
    }

    /**
     * 退出登录：token_version+1 让当前 token 立即失效，并清除会话 cookie。
     */
    @Transactional
    public void logout(Long userId, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        jdbc.update("UPDATE admin_users SET token_version = token_version + 1, updated_at = NOW() WHERE id = ?", userId);
        logService.operation(userId, "认证", "退出登录", httpRequest, Map.of("id", userId), Map.of("success", true));
        clearTokenCookie(httpResponse, httpRequest);
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
        profile.setTokenVersion(Rows.integer(row, "token_version"));
        profile.setMustChangePassword(Rows.integer(row, "must_change_password") == 1);
        profile.setDepartmentName(Rows.str(row, "department_name"));
        profile.setRoles(jdbc.queryForList("""
            SELECT r.name FROM admin_user_roles aur
            JOIN roles r ON r.id = aur.role_id
            WHERE aur.admin_user_id = ?
            ORDER BY r.id
            """, String.class, userId));
        if (Boolean.TRUE.equals(profile.getSuperAdmin())) {
            // 已禁用的权限不下发，避免前端凭 permission_keys 展示已停用的菜单或按钮。
            profile.setPermissionKeys(jdbc.queryForList(
                "SELECT permission_key FROM permissions WHERE status = 1 ORDER BY id", String.class));
        } else {
            profile.setPermissionKeys(jdbc.queryForList("""
                SELECT DISTINCT p.permission_key FROM permissions p
                JOIN role_permissions rp ON rp.permission_id = p.id
                JOIN admin_user_roles aur ON aur.role_id = rp.role_id
                WHERE aur.admin_user_id = ? AND p.status = 1
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

    /**
     * 修改密码：不再信任客户端 md5；成功后 token_version+1 让当前会话立即失效。
     */
    @Transactional
    public void updatePassword(Long userId, AuthRequests.Password request, HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        if (request.getNewPassword().equals(request.getOldPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        if (!request.getNewPassword().matches(PasswordUtils.POLICY_REGEX)) {
            throw new BusinessException(PasswordUtils.POLICY_MESSAGE);
        }
        Map<String, Object> user = jdbc.queryForMap("SELECT * FROM admin_users WHERE id = ?", userId);
        if (!PasswordUtils.verify(request.getOldPassword(), Rows.str(user, "password_hash"), Rows.str(user, "salt"))) {
            throw new BusinessException("原密码错误");
        }
        String salt = PasswordUtils.newSalt();
        String newHash = PasswordUtils.hash(request.getNewPassword(), salt);
        jdbc.update("""
            UPDATE admin_users SET password_hash = ?, salt = ?, must_change_password = 0,
              token_version = token_version + 1, updated_at = NOW(), updated_by = ?
            WHERE id = ?
            """, newHash, salt, userId, userId);
        logService.operation(userId, "个人资料", "修改密码", httpRequest, Map.of("id", userId), Map.of("success", true));
        clearTokenCookie(httpResponse, httpRequest);
    }

    public String tokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (TOKEN_COOKIE.equals(cookie.getName())) {
                String value = cookie.getValue();
                return value == null || value.isBlank() ? null : value;
            }
        }
        return null;
    }

    public void writeTokenCookie(HttpServletResponse response, HttpServletRequest request, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(TOKEN_COOKIE, token)
            .httpOnly(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(jwtTokenService.expireSeconds())
            .secure(isSecure(request))
            .build()
            .toString());
    }

    public void clearTokenCookie(HttpServletResponse response, HttpServletRequest request) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(TOKEN_COOKIE, "")
            .httpOnly(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .secure(isSecure(request))
            .build()
            .toString());
    }

    private boolean isSecure(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return forwardedProto != null && forwardedProto.equalsIgnoreCase("https");
    }

    /**
     * 登录失败统一出口：记录失败次数与审计日志，达到阈值后返回 429，否则返回 401。
     */
    private BusinessException credentialError(String username, String ip, Long userId, HttpServletRequest httpRequest) {
        loginThrottleService.recordFailure(username, ip);
        logService.login(userId, username, 0, "账号或密码错误", httpRequest);
        log.warn("登录失败: username={}, ip={}, 原因=账号或密码错误", username, ip);
        if (loginThrottleService.isLocked(username, ip)) {
            long minutes = loginThrottleService.remainingMinutes(username, ip);
            logService.login(userId, username, 0, "登录失败次数过多，账号已锁定", httpRequest);
            log.warn("登录失败次数达到上限，账号锁定: username={}, ip={}, {}分钟", username, ip, minutes);
            return new BusinessException("登录失败次数过多，请 " + minutes + " 分钟后再试", 429);
        }
        return new BusinessException("账号或密码错误", 401);
    }

    /**
     * 旧数据（salt 8 位）校验通过后自动升级为 PBKDF2。
     */
    private void upgradeLegacyPasswordIfNeeded(Long userId, String plainPassword, String salt) {
        if (!PasswordUtils.isLegacySalt(salt)) {
            return;
        }
        String newSalt = PasswordUtils.newSalt();
        String newHash = PasswordUtils.hash(plainPassword, newSalt);
        jdbc.update("UPDATE admin_users SET password_hash = ?, salt = ?, updated_at = NOW() WHERE id = ?",
            newHash, newSalt, userId);
        log.info("旧密码哈希已自动升级为 PBKDF2: userId={}", userId);
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }
}
