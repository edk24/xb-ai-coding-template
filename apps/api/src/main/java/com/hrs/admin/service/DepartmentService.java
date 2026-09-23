package com.hrs.admin.service;

import com.hrs.admin.dto.DepartmentRequest;
import com.hrs.admin.exception.BusinessException;
import com.hrs.admin.vo.DepartmentNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {
    private final JdbcTemplate jdbc;
    private final QueryService queryService;
    private final LogService logService;

    public DepartmentService(JdbcTemplate jdbc, QueryService queryService, LogService logService) {
        this.jdbc = jdbc;
        this.queryService = queryService;
        this.logService = logService;
    }

    @Transactional
    public DepartmentNode create(DepartmentRequest request, Long operatorId, HttpServletRequest httpRequest) {
        assertParentExists(request.getParentId());
        jdbc.update("""
            INSERT INTO departments (parent_id, name, leader, phone, sort, status, remark, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, request.getParentId(), request.getName(), nz(request.getLeader()), nz(request.getPhone()),
            request.getSort(), request.getStatus(), nz(request.getRemark()), operatorId, operatorId);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        logService.operation(operatorId, "部门管理", "新增部门", httpRequest, Map.of("name", request.getName()), Map.of("id", id));
        return find(id);
    }

    @Transactional
    public DepartmentNode update(Long id, DepartmentRequest request, Long operatorId, HttpServletRequest httpRequest) {
        requireDepartment(id);
        if (id == 1 && request.getParentId() != 0) {
            throw new BusinessException("根部门不能调整上级");
        }
        if (id.equals(request.getParentId())) {
            throw new BusinessException("上级部门不能选择自己");
        }
        assertParentExists(request.getParentId());
        jdbc.update("""
            UPDATE departments SET parent_id = ?, name = ?, leader = ?, phone = ?, sort = ?, status = ?, remark = ?,
              updated_by = ?, updated_at = NOW()
            WHERE id = ?
            """, request.getParentId(), request.getName(), nz(request.getLeader()), nz(request.getPhone()),
            request.getSort(), request.getStatus(), nz(request.getRemark()), operatorId, id);
        logService.operation(operatorId, "部门管理", "编辑部门", httpRequest, Map.of("id", id), Map.of("success", true));
        return find(id);
    }

    @Transactional
    public void delete(Long id, Long operatorId, HttpServletRequest httpRequest) {
        requireDepartment(id);
        if (id == 1) {
            throw new BusinessException("根部门不允许删除");
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM departments WHERE parent_id = ?", Long.class, id) > 0) {
            throw new BusinessException("存在子部门，不能删除");
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM admin_users WHERE department_id = ?", Long.class, id) > 0) {
            throw new BusinessException("存在管理员归属，不能删除");
        }
        jdbc.update("DELETE FROM departments WHERE id = ?", id);
        logService.operation(operatorId, "部门管理", "删除部门", httpRequest, Map.of("id", id), Map.of("success", true));
    }

    private DepartmentNode find(Long id) {
        return queryService.departmentFlat().stream()
            .filter(dept -> dept.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new BusinessException("部门不存在", 404));
    }

    private void requireDepartment(Long id) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM departments WHERE id = ?", Long.class, id) == 0) {
            throw new BusinessException("部门不存在", 404);
        }
    }

    private void assertParentExists(Long parentId) {
        if (parentId != null && parentId != 0 && jdbc.queryForObject("SELECT COUNT(*) FROM departments WHERE id = ?", Long.class, parentId) == 0) {
            throw new BusinessException("上级部门不存在");
        }
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }
}
