package com.hrs.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogService {
    private static final Logger log = LoggerFactory.getLogger(LogService.class);
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public LogService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /**
     * 审计日志使用独立事务（REQUIRES_NEW）：业务回滚时日志仍然落库，
     * 例如登录失败后外层事务回滚，失败登录记录不会被一起回滚掉。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void login(Long adminUserId, String username, int status, String failReason, HttpServletRequest request) {
        try {
            jdbc.update("""
                INSERT INTO login_logs (admin_user_id, username_snapshot, ip, user_agent, status, fail_reason, login_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                """,
                adminUserId, username, ip(request), userAgent(request), status, failReason == null ? "" : failReason);
        } catch (RuntimeException e) {
            // 审计失败不能反过来影响登录/业务主流程，但也不能完全静默。
            log.warn("写入登录日志失败: username={}", username, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void operation(Long adminUserId, String module, String action, HttpServletRequest request,
                          Object requestSummary, Object responseSummary) {
        try {
            jdbc.update("""
                INSERT INTO operation_logs (admin_user_id, module, action, method, path, request_summary, response_summary, ip, operated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """,
                adminUserId, module, action, request.getMethod(), request.getRequestURI(),
                json(requestSummary), json(responseSummary), ip(request));
        } catch (RuntimeException e) {
            log.warn("写入操作日志失败: module={}, action={}", module, action, e);
        }
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
