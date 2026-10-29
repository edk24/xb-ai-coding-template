package com.hrs.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrs.admin.common.PasswordUtils;
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

/**
 * 附件受鉴权下载与配置密钥脱敏的回归测试。
 * 登录按服务端新契约传明文密码；由于种子 admin 是历史弱口令，登录后会被强制改密并拦截其它接口，
 * 所以每个用例前先用 JDBC 把 admin 换成强口令并清掉 must_change_password（与 AdminApiIntegrationTest 写法一致）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttachmentAccessTest {
    private static final String ADMIN_PASSWORD = "Admin12345";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareSeedAdmin() {
        String salt = PasswordUtils.newSalt();
        jdbcTemplate.update("""
            UPDATE admin_users
            SET password_hash = ?, salt = ?, must_change_password = 0, token_version = token_version + 1
            WHERE id = 1
            """, PasswordUtils.hash(ADMIN_PASSWORD, salt), salt);
    }

    @Test
    void anonymousCannotListAttachments() throws Exception {
        mockMvc.perform(get("/admin-api/attachments"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void attachmentListExposesFileUrlButNotServerPath() throws Exception {
        // 插一条本地附件记录：列表只应返回受鉴权的 file_url，不能把服务端 path 暴露出去。
        jdbcTemplate.update("""
            INSERT INTO attachments (admin_user_id, name, size, mime_type, storage_type, path, url, created_at)
            VALUES (1, '测试附件.txt', 5, 'text/plain', 'local', '2026/01/01/test.txt', '', NOW())
            """);
        Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM attachments", Long.class);

        String token = login();
        String body = mockMvc.perform(get("/admin-api/attachments").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn().getResponse().getContentAsString();

        JsonNode items = objectMapper.readTree(body).path("data").path("items");
        JsonNode target = null;
        for (JsonNode item : items) {
            if (item.path("id").asLong() == id) {
                target = item;
            }
        }
        assertThat(target).isNotNull();
        assertThat(target.has("file_url")).isTrue();
        assertThat(target.has("path")).isFalse();
        assertThat(target.path("file_url").asText()).isEqualTo("/admin-api/attachments/" + id + "/file");
    }

    @Test
    void recordRejectsUnsafeAttachmentUrl() throws Exception {
        String token = login();
        mockMvc.perform(post("/admin-api/attachments/record")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"storage_type":"cos","name":"x.txt","path":"uploads/x.txt","size":1,
                     "mime_type":"text/plain","url":"javascript:alert(1)"}
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value(1))
            .andExpect(jsonPath("$.message").value("附件地址不合法"));
    }

    @Test
    void configSecretIsMaskedWithHasValueFlag() throws Exception {
        String token = login();
        String body = mockMvc.perform(get("/admin-api/config/items").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn().getResponse().getContentAsString();

        JsonNode target = null;
        for (JsonNode item : objectMapper.readTree(body).path("data")) {
            if ("cos_secret_key".equals(item.path("key").asText())) {
                target = item;
            }
        }
        assertThat(target).isNotNull();
        assertThat(target.path("value").asText()).isEmpty();
        assertThat(target.path("has_value").asBoolean()).isFalse();
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"%s"}
                    """.formatted(ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data").path("token").asText("");
        assertThat(token).isNotBlank();
        return token;
    }
}
