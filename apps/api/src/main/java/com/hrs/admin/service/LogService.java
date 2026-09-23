package com.hrs.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LogService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public LogService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void login(Long adminUserId, String username, int status, String failReason, HttpServletRequest request) {
        jdbc.update("""
            INSERT INTO login_logs (admin_user_id, username_snapshot, ip, user_agent, status, fail_reason, login_at)
            VALUES (?, ?, ?, ?, ?, ?, NOW())
            """,
            adminUserId, username, ip(request), userAgent(request), status, failReason == null ? "" : failReason);
    }

    public void operation(Long adminUserId, String module, String action, HttpServletRequest request,
                          Object requestSummary, Object responseSummary) {
        jdbc.update("""
            INSERT INTO operation_logs (admin_user_id, module, action, method, path, request_summary, response_summary, ip, operated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """,
            adminUserId, module, action, request.getMethod(), request.getRequestURI(),
            json(requestSummary), json(responseSummary), ip(request));
    }

    public String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        String value = request.getHeader("User-Agent");
        return value == null ? "" : value;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
