package com.hrs.admin.service;

import com.hrs.admin.vo.AdminUserRow;
import com.hrs.admin.vo.AttachmentRow;
import com.hrs.admin.vo.ConfigItemRow;
import com.hrs.admin.vo.DepartmentNode;
import com.hrs.admin.vo.PermissionNode;
import com.hrs.admin.vo.RoleRow;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.hrs.admin.mapper.AdminMapper;
import org.springframework.stereotype.Service;

@Service
public class QueryService {
    private final AdminMapper adminMapper;

    public QueryService(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    public List<AdminUserRow> adminUsers() {
        List<AdminUserRow> users = adminMapper.selectAdminUsers().stream().map(this::adminUserRow).toList();
        Map<Long, List<Map<String, Object>>> roleRows = adminMapper.selectAdminUserRoles()
            .stream().collect(Collectors.groupingBy(row -> Rows.lng(row, "admin_user_id")));
        for (AdminUserRow user : users) {
            List<Map<String, Object>> rows = roleRows.getOrDefault(user.getId(), List.of());
            user.setRoles(rows.stream().map(row -> Rows.str(row, "name")).toList());
            user.setRoleIds(rows.stream().map(row -> Rows.lng(row, "id")).toList());
        }
        return users;
    }

    public List<RoleRow> roles() {
        List<RoleRow> roles = adminMapper.selectRoles().stream().map(this::roleRow).toList();
        Map<Long, List<Long>> permissionMap = adminMapper.selectRolePermissions()
            .stream().collect(Collectors.groupingBy(
                row -> Rows.lng(row, "role_id"),
                Collectors.mapping(row -> Rows.lng(row, "permission_id"), Collectors.toList())));
        for (RoleRow role : roles) {
            role.setPermissionIds(permissionMap.getOrDefault(role.getId(), List.of()));
        }
        return roles;
    }

    public List<DepartmentNode> departmentTree() {
        return TreeBuilder.departmentTree(departmentFlat());
    }

    public List<DepartmentNode> departmentFlat() {
        return adminMapper.selectDepartments().stream().map(this::departmentNode).toList();
    }

    public List<PermissionNode> permissionTree() {
        return TreeBuilder.permissionTree(permissionFlat(false, null));
    }

    public List<PermissionNode> menuTreeForUser(Long userId, boolean superAdmin) {
        return TreeBuilder.permissionTree(permissionFlat(true, superAdmin ? null : userId));
    }

    public List<PermissionNode> permissionFlat(boolean enabledOnly, Long userId) {
        return adminMapper.selectPermissions(enabledOnly, userId).stream().map(this::permissionNode).toList();
    }

    public List<ConfigItemRow> configItems() {
        return adminMapper.selectConfigItems().stream().map(this::configItemRow).toList();
    }

    public List<AttachmentRow> attachments(int page, int pageSize, String storageType) {
        int offset = (page - 1) * pageSize;
        return adminMapper.selectAttachments(pageSize, offset, storageType).stream().map(this::attachmentRow).toList();
    }

    public long attachmentCount(String storageType) {
        return adminMapper.countAttachments(storageType);
    }

    public List<Map<String, Object>> loginLogs() {
        return adminMapper.selectLoginLogs();
    }

    public List<Map<String, Object>> operationLogs() {
        return adminMapper.selectOperationLogs();
    }

    private AdminUserRow adminUserRow(Map<String, Object> row) {
        AdminUserRow user = new AdminUserRow();
        user.setId(Rows.lng(row, "id"));
        user.setDepartmentId(Rows.lng(row, "department_id"));
        user.setUsername(Rows.str(row, "username"));
        user.setNickname(Rows.str(row, "nickname"));
        user.setAvatar(Rows.str(row, "avatar"));
        user.setPhone(Rows.str(row, "phone"));
        user.setEmail(Rows.str(row, "email"));
        user.setStatus(Rows.integer(row, "status"));
        user.setSuperFlag(Rows.integer(row, "is_super"));
        user.setRemark(Rows.str(row, "remark"));
        user.setCreatedAt(Rows.date(row, "created_at"));
        user.setDepartmentName(Rows.str(row, "department_name"));
        return user;
    }

    private RoleRow roleRow(Map<String, Object> row) {
        RoleRow role = new RoleRow();
        role.setId(Rows.lng(row, "id"));
        role.setName(Rows.str(row, "name"));
        role.setCode(Rows.str(row, "code"));
        role.setStatus(Rows.integer(row, "status"));
        role.setDataScope(Rows.integer(row, "data_scope"));
        role.setRemark(Rows.str(row, "remark"));
        role.setCreatedAt(Rows.date(row, "created_at"));
        role.setUpdatedAt(Rows.date(row, "updated_at"));
        return role;
    }

    private DepartmentNode departmentNode(Map<String, Object> row) {
        DepartmentNode node = new DepartmentNode();
        node.setId(Rows.lng(row, "id"));
        node.setParentId(Rows.lng(row, "parent_id"));
        node.setName(Rows.str(row, "name"));
        node.setLeader(Rows.str(row, "leader"));
        node.setPhone(Rows.str(row, "phone"));
        node.setSort(Rows.integer(row, "sort"));
        node.setStatus(Rows.integer(row, "status"));
        node.setRemark(Rows.str(row, "remark"));
        return node;
    }

    private PermissionNode permissionNode(Map<String, Object> row) {
        PermissionNode node = new PermissionNode();
        node.setId(Rows.lng(row, "id"));
        node.setParentId(Rows.lng(row, "parent_id"));
        node.setName(Rows.str(row, "name"));
        node.setType(Rows.str(row, "type"));
        node.setRoutePath(Rows.str(row, "route_path"));
        node.setComponentPath(Rows.str(row, "component_path"));
        node.setPermissionKey(Rows.str(row, "permission_key"));
        node.setIcon(Rows.str(row, "icon"));
        node.setSort(Rows.integer(row, "sort"));
        node.setHidden(Rows.integer(row, "hidden"));
        node.setStatus(Rows.integer(row, "status"));
        node.setRemark(Rows.str(row, "remark"));
        return node;
    }

    private ConfigItemRow configItemRow(Map<String, Object> row) {
        ConfigItemRow item = new ConfigItemRow();
        item.setId(Rows.lng(row, "id"));
        item.setGroupName(Rows.str(row, "group_name"));
        item.setName(Rows.str(row, "name"));
        item.setKey(Rows.str(row, "key"));
        item.setValue(Rows.str(row, "value"));
        item.setType(Rows.str(row, "type"));
        item.setOptions(Rows.str(row, "options"));
        item.setSort(Rows.integer(row, "sort"));
        item.setStatus(Rows.integer(row, "status"));
        item.setRemark(Rows.str(row, "remark"));
        item.setCreatedAt(Rows.date(row, "created_at"));
        return item;
    }

    private AttachmentRow attachmentRow(Map<String, Object> row) {
        AttachmentRow item = new AttachmentRow();
        item.setId(Rows.lng(row, "id"));
        item.setAdminUserId(Rows.lng(row, "admin_user_id"));
        item.setName(Rows.str(row, "name"));
        item.setSize(Rows.lng(row, "size"));
        item.setMimeType(Rows.str(row, "mime_type"));
        item.setStorageType(Rows.str(row, "storage_type"));
        item.setPath(Rows.str(row, "path"));
        item.setUrl(Rows.str(row, "url"));
        item.setCreatedAt(Rows.date(row, "created_at"));
        return item;
    }
}
