package com.hrs.admin.service;

import com.hrs.admin.dto.PermissionRequest;
import com.hrs.admin.exception.BusinessException;
import com.hrs.admin.vo.PermissionNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionService {
    private final JdbcTemplate jdbc;
    private final QueryService queryService;
    private final LogService logService;

    public PermissionService(JdbcTemplate jdbc, QueryService queryService, LogService logService) {
        this.jdbc = jdbc;
        this.queryService = queryService;
        this.logService = logService;
    }

    @Transactional
    public PermissionNode create(PermissionRequest request, Long operatorId, HttpServletRequest httpRequest) {
        validateHierarchy(request.getParentId(), request.getType());
        assertUniqueKey(request.getPermissionKey(), null);
        jdbc.update("""
            INSERT INTO permissions (parent_id, name, type, route_path, component_path, permission_key, icon, sort, hidden, status, remark, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, request.getParentId(), request.getName(), request.getType(), nz(request.getRoutePath()),
            nz(request.getComponentPath()), request.getPermissionKey(), nz(request.getIcon()), request.getSort(),
            request.getHidden(), request.getStatus(), nz(request.getRemark()), operatorId, operatorId);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        logService.operation(operatorId, "菜单权限", "新增权限", httpRequest, Map.of("name", request.getName()), Map.of("id", id));
        return find(id);
    }

    @Transactional
    public PermissionNode update(Long id, PermissionRequest request, Long operatorId, HttpServletRequest httpRequest) {
        requirePermission(id);
        validateHierarchy(request.getParentId(), request.getType());
        assertUniqueKey(request.getPermissionKey(), id);
        if (id <= 3) {
            String oldKey = jdbc.queryForObject("SELECT permission_key FROM permissions WHERE id = ?", String.class, id);
            if (!oldKey.equals(request.getPermissionKey())) {
                throw new BusinessException("内置权限标识不允许修改");
            }
        }
        jdbc.update("""
            UPDATE permissions SET parent_id = ?, name = ?, type = ?, route_path = ?, component_path = ?, permission_key = ?,
              icon = ?, sort = ?, hidden = ?, status = ?, remark = ?, updated_by = ?, updated_at = NOW()
            WHERE id = ?
            """, request.getParentId(), request.getName(), request.getType(), nz(request.getRoutePath()),
            nz(request.getComponentPath()), request.getPermissionKey(), nz(request.getIcon()), request.getSort(),
            request.getHidden(), request.getStatus(), nz(request.getRemark()), operatorId, id);
        logService.operation(operatorId, "菜单权限", "编辑权限", httpRequest, Map.of("id", id), Map.of("success", true));
        return find(id);
    }

    @Transactional
    public void delete(Long id, Long operatorId, HttpServletRequest httpRequest) {
        requirePermission(id);
        if (id <= 23) {
            throw new BusinessException("内置权限节点不允许删除");
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM permissions WHERE parent_id = ?", Long.class, id) > 0) {
            throw new BusinessException("存在子节点，不能删除");
        }
        jdbc.update("DELETE FROM role_permissions WHERE permission_id = ?", id);
        jdbc.update("DELETE FROM permissions WHERE id = ?", id);
        logService.operation(operatorId, "菜单权限", "删除权限", httpRequest, Map.of("id", id), Map.of("success", true));
    }

    private PermissionNode find(Long id) {
        return queryService.permissionFlat(false, null).stream()
            .filter(node -> node.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new BusinessException("权限不存在", 404));
    }

    private void requirePermission(Long id) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM permissions WHERE id = ?", Long.class, id) == 0) {
            throw new BusinessException("权限不存在", 404);
        }
    }

    private void validateHierarchy(Long parentId, String type) {
        if ("catalog".equals(type)) {
            if (parentId != null && parentId != 0) {
                throw new BusinessException("目录只能作为顶级节点");
            }
            return;
        }
        if (parentId == null || parentId == 0) {
            throw new BusinessException("请选择父级节点");
        }
        String parentType = jdbc.queryForObject("SELECT type FROM permissions WHERE id = ?", String.class, parentId);
        if ("menu".equals(type) && !"catalog".equals(parentType)) {
            throw new BusinessException("菜单节点只能挂在目录下");
        }
        if ("button".equals(type) && !"menu".equals(parentType)) {
            throw new BusinessException("按钮节点只能挂在菜单下");
        }
        if (!"catalog".equals(type) && !"menu".equals(type) && !"button".equals(type)) {
            throw new BusinessException("权限类型不正确");
        }
    }

    private void assertUniqueKey(String key, Long ignoreId) {
        Long count = ignoreId == null
            ? jdbc.queryForObject("SELECT COUNT(*) FROM permissions WHERE permission_key = ?", Long.class, key)
            : jdbc.queryForObject("SELECT COUNT(*) FROM permissions WHERE permission_key = ? AND id <> ?", Long.class, key, ignoreId);
        if (count != null && count > 0) {
            throw new BusinessException("权限标识已存在");
        }
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }
}
