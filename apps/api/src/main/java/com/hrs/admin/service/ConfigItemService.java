package com.hrs.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrs.admin.dto.ConfigItemRequest;
import com.hrs.admin.exception.BusinessException;
import com.hrs.admin.mapper.AdminMapper;
import com.hrs.admin.vo.ConfigItemRow;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigItemService {
    private final JdbcTemplate jdbc;
    private final QueryService queryService;
    private final LogService logService;
    private final ObjectMapper objectMapper;
    private final AdminMapper adminMapper;

    public ConfigItemService(JdbcTemplate jdbc, QueryService queryService, LogService logService, ObjectMapper objectMapper,
                             AdminMapper adminMapper) {
        this.jdbc = jdbc;
        this.queryService = queryService;
        this.logService = logService;
        this.objectMapper = objectMapper;
        this.adminMapper = adminMapper;
    }

    @Transactional
    public ConfigItemRow create(ConfigItemRequest request, Long operatorId, HttpServletRequest httpRequest) {
        assertUniqueKey(request.getKey(), null);
        jdbc.update("""
            INSERT INTO config_items (name, group_name, `key`, `value`, type, options, sort, status, remark, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, request.getName(), nz(request.getGroupName()), request.getKey(), stringify(request.getValue()),
            request.getType(), nz(request.getOptions()), request.getSort(), request.getStatus(), nz(request.getRemark()), operatorId, operatorId);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        logService.operation(operatorId, "系统配置", "新增配置项", httpRequest, Map.of("key", request.getKey()), Map.of("id", id));
        return find(id);
    }

    @Transactional
    public ConfigItemRow update(Long id, ConfigItemRequest request, Long operatorId, HttpServletRequest httpRequest) {
        requireItem(id);
        assertUniqueKey(request.getKey(), id);
        jdbc.update("""
            UPDATE config_items SET name = ?, group_name = ?, `key` = ?, `value` = ?, type = ?, options = ?, sort = ?,
              status = ?, remark = ?, updated_by = ?, updated_at = NOW()
            WHERE id = ?
            """, request.getName(), nz(request.getGroupName()), request.getKey(), stringify(request.getValue()),
            request.getType(), nz(request.getOptions()), request.getSort(), request.getStatus(), nz(request.getRemark()), operatorId, id);
        logService.operation(operatorId, "系统配置", "编辑配置项", httpRequest, Map.of("id", id), Map.of("success", true));
        return find(id);
    }

    @Transactional
    public void batchSave(List<Map<String, Object>> items, Long operatorId, HttpServletRequest httpRequest) {
        for (Map<String, Object> item : items) {
            Object idValue = item.get("id");
            if (!(idValue instanceof Number number)) {
                continue;
            }
            jdbc.update("UPDATE config_items SET `value` = ?, updated_by = ?, updated_at = NOW() WHERE id = ?",
                stringify(item.get("value")), operatorId, number.longValue());
        }
        logService.operation(operatorId, "系统配置", "批量保存配置值", httpRequest, Map.of("count", items.size()), Map.of("success", true));
    }

    @Transactional
    public void delete(Long id, Long operatorId, HttpServletRequest httpRequest) {
        requireItem(id);
        jdbc.update("DELETE FROM config_items WHERE id = ?", id);
        logService.operation(operatorId, "系统配置", "删除配置项", httpRequest, Map.of("id", id), Map.of("success", true));
    }

    public String configValue(String key, String fallback) {
        List<String> values = adminMapper.selectConfigValue(key);
        if (values.isEmpty() || values.getFirst() == null || values.getFirst().isBlank()) {
            return fallback;
        }
        return values.getFirst();
    }

    private ConfigItemRow find(Long id) {
        return queryService.configItems().stream()
            .filter(item -> item.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new BusinessException("配置项不存在", 404));
    }

    private void requireItem(Long id) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM config_items WHERE id = ?", Long.class, id) == 0) {
            throw new BusinessException("配置项不存在", 404);
        }
    }

    private void assertUniqueKey(String key, Long ignoreId) {
        Long count = ignoreId == null
            ? jdbc.queryForObject("SELECT COUNT(*) FROM config_items WHERE `key` = ?", Long.class, key)
            : jdbc.queryForObject("SELECT COUNT(*) FROM config_items WHERE `key` = ? AND id <> ?", Long.class, key, ignoreId);
        if (count != null && count > 0) {
            throw new BusinessException("配置项键名已存在");
        }
    }

    private String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String string) {
            return string;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }
}
