package com.hrs.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrs.admin.common.PasswordUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanLoginAndReadProfileDashboardAndMenuTree() throws Exception {
        String response = mockMvc.perform(post("/admin-api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"%s"}
                    """.formatted(PasswordUtils.md5Hex("123456"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.user.username").value("admin"))
            .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        String token = root.at("/data/token").asText();
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/admin-api/auth/profile").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.permission_keys[0]").value("dashboard:view"));
        mockMvc.perform(get("/admin-api/meta/dashboard").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.stats.admin_users").value(1));
        mockMvc.perform(get("/admin-api/meta/menu-tree").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("仪表盘"));
    }

    @Test
    void adminCanManageScheduledJobs() throws Exception {
        String token = loginToken();

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

    private String loginToken() throws Exception {
        String response = mockMvc.perform(post("/admin-api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"%s"}
                    """.formatted(PasswordUtils.md5Hex("123456"))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).at("/data/token").asText();
    }
}
