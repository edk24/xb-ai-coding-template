package com.hrs.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrs.admin.common.PasswordUtils;
import com.hrs.admin.service.LoginThrottleService;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminApiIntegrationTest {
    private static final String ADMIN_PASSWORD = "Admin12345";
    private static final String USER_PASSWORD = "abc12345";
    private static final String ACCOUNT_ERROR = "账号或密码错误";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private LoginThrottleService loginThrottleService;

    private final List<String> testUsernames = new ArrayList<>();

    @BeforeEach
    void prepareSeedAdmin() {
        // V2 种子的 admin 是历史弱密码 123456，V3 会把它标记为必须改密；
        // 这里换成强口令，避免每个用例都触发弱密码兜底或强制改密流程（弱密码场景有专门用例覆盖）
        String salt = PasswordUtils.newSalt();
        jdbc.update("UPDATE admin_users SET password_hash = ?, salt = ?, must_change_password = 0 WHERE id = 1",
            PasswordUtils.hash(ADMIN_PASSWORD, salt), salt);
    }

    @AfterEach
    void restoreSeedAdminAndCleanTestData() {
        // 用例可能把 admin 还原成弱密码，这里统一恢复为强密码，避免影响共享同一个 Spring 上下文的其它测试类
        String salt = PasswordUtils.newSalt();
        jdbc.update("UPDATE admin_users SET password_hash = ?, salt = ?, must_change_password = 0 WHERE id = 1",
            PasswordUtils.hash(ADMIN_PASSWORD, salt), salt);
        for (String username : testUsernames) {
            jdbc.update("DELETE FROM admin_user_roles WHERE admin_user_id IN (SELECT id FROM admin_users WHERE username = ?)", username);
            jdbc.update("DELETE FROM admin_users WHERE username = ?", username);
            jdbc.update("DELETE FROM login_logs WHERE username_snapshot = ?", username);
        }
        testUsernames.clear();
        loginThrottleService.clear("throttle-test-user", "127.0.0.1");
    }

    @Test
    void adminCanLoginWithPlainPasswordAndUseTokenAndCookie() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/admin-api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"%s"}
                    """.formatted(ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.user.username").value("admin"))
            .andExpect(jsonPath("$.data.user.must_change_password").value(false))
            .andReturn();

        String setCookie = loginResult.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie)
            .contains("ADMIN_TOKEN=")
            .contains("HttpOnly")
            .contains("SameSite=Lax")
            .contains("Path=/");
        String cookieToken = extractCookieToken(setCookie);
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).at("/data/token").asText();
        assertThat(token).isNotBlank().isEqualTo(cookieToken);

        mockMvc.perform(get("/admin-api/auth/profile").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.permission_keys[0]").value("dashboard:view"));
        mockMvc.perform(get("/admin-api/meta/dashboard").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.stats.admin_users").value(1));
        mockMvc.perform(get("/admin-api/meta/menu-tree").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("仪表盘"));
        // cookie 同样可作为登录态
        mockMvc.perform(get("/admin-api/auth/profile").cookie(new Cookie("ADMIN_TOKEN", cookieToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void adminCanManageScheduledJobs() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/admin-api/scheduled-jobs").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].code").value("system_noop"));
        mockMvc.perform(get("/admin-api/scheduled-jobs/handlers").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].value").value("system.noop"));
        mockMvc.perform(post("/admin-api/scheduled-jobs/1/run").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("success"));
        mockMvc.perform(get("/admin-api/scheduled-jobs/logs?job_id=1").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].status").value("success"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/admin-api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"wrong-password-1"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value(ACCOUNT_ERROR));
    }

    @Test
    void loginAfterTooManyFailuresReturns429() throws Exception {
        testUsernames.add("throttle-test-user");
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/admin-api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"username":"throttle-test-user","password":"wrong-password-1"}
                        """))
                .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/admin-api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"throttle-test-user","password":"wrong-password-1"}
                    """))
            .andExpect(status().is(429))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("分钟")));
    }

    @Test
    void disabledAccountCannotLogin() throws Exception {
        insertUser("disabled-test-user", USER_PASSWORD, 0);

        mockMvc.perform(post("/admin-api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"disabled-test-user","password":"%s"}
                    """.formatted(USER_PASSWORD)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("账号已禁用"));
    }

    @Test
    void userWithoutPermissionGets403OnAdminApi() throws Exception {
        insertUser("no-perm-test-user", USER_PASSWORD, 1);
        String token = loginToken("no-perm-test-user", USER_PASSWORD);

        mockMvc.perform(post("/admin-api/admin-users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"department_id":1,"username":"should-not-created","nickname":"无权限","status":1,"password":"abc12345"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("没有该操作权限"));
    }

    @Test
    void legacyMd5PasswordIsUpgradedAfterSuccessfulLogin() throws Exception {
        String salt = "9f2f5c0d";
        String legacyHash = PasswordUtils.md5Hex(PasswordUtils.md5Hex(USER_PASSWORD) + salt);
        insertUserRow("legacy-test-user", legacyHash, salt, 1, 0);

        loginToken("legacy-test-user", USER_PASSWORD);

        String newSalt = jdbc.queryForObject("SELECT salt FROM admin_users WHERE username = ?", String.class, "legacy-test-user");
        String newHash = jdbc.queryForObject("SELECT password_hash FROM admin_users WHERE username = ?", String.class, "legacy-test-user");
        assertThat(newSalt).hasSize(32);
        assertThat(newHash).hasSize(32);
        assertThat(PasswordUtils.verify(USER_PASSWORD, newHash, newSalt)).isTrue();
    }

    @Test
    void weakDefaultPasswordForcesPasswordChange() throws Exception {
        // V2 种子：admin 的初始密码是 123456（legacy md5 + 8 位 salt）。模拟库被恢复回默认密码后重新登录。
        jdbc.update("UPDATE admin_users SET password_hash = ?, salt = ?, must_change_password = 0 WHERE id = 1",
            PasswordUtils.md5Hex(PasswordUtils.md5Hex("123456") + "9f2f5c0d"), "9f2f5c0d");

        MvcResult loginResult = mockMvc.perform(post("/admin-api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"123456"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.must_change_password").value(true))
            .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).at("/data/token").asText();

        // 允许查资料，但受保护接口一律 403，强制先改密
        mockMvc.perform(get("/admin-api/auth/profile").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.must_change_password").value(true));
        mockMvc.perform(get("/admin-api/meta/dashboard").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("请先修改初始密码"));
    }

    @Test
    void weakDefaultPasswordUserIsForcedToChangeToo() throws Exception {
        insertUser("weak-default-test-user", "123456", 1);
        String token = loginToken("weak-default-test-user", "123456");

        mockMvc.perform(get("/admin-api/auth/profile").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.must_change_password").value(true));
        mockMvc.perform(post("/admin-api/admin-users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"department_id":1,"username":"should-not-created","nickname":"强制改密","status":1,"password":"abc12345"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("请先修改初始密码"));
    }

    @Test
    void changePasswordRequiresPolicyAndInvalidatesCurrentToken() throws Exception {
        insertUser("change-pwd-test-user", USER_PASSWORD, 1);
        String token = loginToken("change-pwd-test-user", USER_PASSWORD);

        mockMvc.perform(put("/admin-api/auth/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"old_password":"%s","new_password":"12345678","confirm_password":"12345678"}
                    """.formatted(USER_PASSWORD)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message").value("密码需 8–64 位且同时包含字母和数字"));
        mockMvc.perform(put("/admin-api/auth/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"old_password":"%s","new_password":"Newpass12345","confirm_password":"Newpass1234"}
                    """.formatted(USER_PASSWORD)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message").value("两次输入的新密码不一致"));
        mockMvc.perform(put("/admin-api/auth/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"old_password":"%s","new_password":"Newpass12345","confirm_password":"Newpass12345"}
                    """.formatted(USER_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("密码修改成功，请重新登录"));

        // 改密后 token_version+1，旧 token 立即失效
        mockMvc.perform(get("/admin-api/auth/profile").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("登录状态已失效，请重新登录"));
        String newToken = loginToken("change-pwd-test-user", "Newpass12345");
        mockMvc.perform(get("/admin-api/auth/profile").header("Authorization", "Bearer " + newToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.must_change_password").value(false));
    }

    @Test
    void logoutInvalidatesTokenAndClearsCookie() throws Exception {
        insertUser("logout-test-user", USER_PASSWORD, 1);
        String token = loginToken("logout-test-user", USER_PASSWORD);

        MvcResult logoutResult = mockMvc.perform(post("/admin-api/auth/logout")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        assertThat(logoutResult.getResponse().getHeader("Set-Cookie")).contains("ADMIN_TOKEN=").contains("Max-Age=0");

        mockMvc.perform(get("/admin-api/auth/profile").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("登录状态已失效，请重新登录"));
    }

    @Test
    void resetPasswordRequiresValidNewPasswordAndForcesChange() throws Exception {
        String adminToken = adminToken();
        // 创建用户时密码必填且必须符合策略
        mockMvc.perform(post("/admin-api/admin-users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"department_id":1,"username":"created-test-user","nickname":"重置密码用户","status":1}
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message").value("请输入登录密码"));
        mockMvc.perform(post("/admin-api/admin-users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"department_id":1,"username":"created-test-user","nickname":"重置密码用户","status":1,"password":"123"}
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message").value("密码需 8–64 位且同时包含字母和数字"));

        MvcResult createResult = mockMvc.perform(post("/admin-api/admin-users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"department_id":1,"username":"created-test-user","nickname":"重置密码用户","status":1,"password":"abc12345","role_ids":[]}
                    """))
            .andExpect(status().isOk())
            .andReturn();
        testUsernames.add("created-test-user");
        long userId = objectMapper.readTree(createResult.getResponse().getContentAsString()).at("/data/id").asLong();

        // 不再回落到 123456，缺参/弱密码都返回 422
        mockMvc.perform(put("/admin-api/admin-users/" + userId + "/reset-password")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(put("/admin-api/admin-users/" + userId + "/reset-password")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"new_password":"123"}
                    """))
            .andExpect(status().isUnprocessableEntity());

        MvcResult resetResult = mockMvc.perform(put("/admin-api/admin-users/" + userId + "/reset-password")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"new_password":"Reset12345"}
                    """))
            .andExpect(status().isOk())
            .andReturn();
        assertThat(resetResult.getResponse().getContentAsString()).doesNotContain("Reset12345");

        // 重置后必须改密，且目标用户已有会话被踢掉
        String userToken = loginToken("created-test-user", "Reset12345");
        mockMvc.perform(get("/admin-api/auth/profile").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.must_change_password").value(true));
        mockMvc.perform(post("/admin-api/admin-users")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"department_id":1,"username":"should-not-created","nickname":"未改密","status":1,"password":"abc12345"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("请先修改初始密码"));
    }

    private String adminToken() throws Exception {
        return loginToken("admin", ADMIN_PASSWORD);
    }

    private String loginToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/admin-api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"%s"}
                    """.formatted(username, password)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        String token = root.at("/data/token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private void insertUser(String username, String plainPassword, int status) {
        String salt = PasswordUtils.newSalt();
        insertUserRow(username, PasswordUtils.hash(plainPassword, salt), salt, status, 0);
    }

    private void insertUserRow(String username, String passwordHash, String salt, int status, int superFlag) {
        testUsernames.add(username);
        jdbc.update("""
            INSERT INTO admin_users (department_id, username, nickname, password_hash, salt, status, is_super, remark)
            VALUES (1, ?, ?, ?, ?, ?, ?, 'integration test user')
            """, username, username, passwordHash, salt, status, superFlag);
    }

    private String extractCookieToken(String setCookie) {
        String prefix = "ADMIN_TOKEN=";
        int start = setCookie.indexOf(prefix);
        int end = setCookie.indexOf(';', start);
        return end < 0 ? setCookie.substring(start + prefix.length()) : setCookie.substring(start + prefix.length(), end);
    }
}
