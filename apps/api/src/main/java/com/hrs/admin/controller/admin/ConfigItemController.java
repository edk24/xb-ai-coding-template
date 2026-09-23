package com.hrs.admin.controller.admin;

import com.hrs.admin.common.ApiResponse;
import com.hrs.admin.dto.ConfigItemRequest;
import com.hrs.admin.security.AdminContext;
import com.hrs.admin.service.ConfigItemService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/config/items")
public class ConfigItemController {
    private final ConfigItemService configItemService;

    public ConfigItemController(ConfigItemService configItemService) {
        this.configItemService = configItemService;
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody ConfigItemRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(configItemService.create(request, AdminContext.get().getId(), httpRequest), "配置项已创建");
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody ConfigItemRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(configItemService.update(id, request, AdminContext.get().getId(), httpRequest), "配置项已更新");
    }

    @PutMapping("/batch-save")
    @SuppressWarnings("unchecked")
    public ApiResponse<Void> batchSave(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        Object items = request.get("items");
        List<Map<String, Object>> rows = items instanceof List<?> list
            ? (List<Map<String, Object>>) (List<?>) list
            : List.of();
        configItemService.batchSave(rows, AdminContext.get().getId(), httpRequest);
        return ApiResponse.ok(null, "配置已保存");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        configItemService.delete(id, AdminContext.get().getId(), httpRequest);
        return ApiResponse.ok(null, "配置项已删除");
    }
}
