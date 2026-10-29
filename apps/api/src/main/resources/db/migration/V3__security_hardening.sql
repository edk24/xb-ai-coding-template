ALTER TABLE admin_users ADD COLUMN token_version INT NOT NULL DEFAULT 0;
ALTER TABLE admin_users ADD COLUMN must_change_password TINYINT NOT NULL DEFAULT 0;
UPDATE admin_users SET must_change_password = 1 WHERE id = 1;
-- 按 permission_key 幂等插入：不写死主键，避免老库里已有自定义权限（id >= 29）时主键冲突
INSERT INTO permissions (parent_id, name, type, route_path, component_path, permission_key, icon, sort, hidden, status, remark)
SELECT 3, '删除管理员', 'button', '', '', 'admin-users:delete', '', 4, 0, 1, ''
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_key = 'admin-users:delete');
INSERT INTO permissions (parent_id, name, type, route_path, component_path, permission_key, icon, sort, hidden, status, remark)
SELECT 7, '新增角色', 'button', '', '', 'roles:create', '', 2, 0, 1, ''
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_key = 'roles:create');
INSERT INTO permissions (parent_id, name, type, route_path, component_path, permission_key, icon, sort, hidden, status, remark)
SELECT 7, '编辑角色', 'button', '', '', 'roles:update', '', 3, 0, 1, ''
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_key = 'roles:update');
INSERT INTO permissions (parent_id, name, type, route_path, component_path, permission_key, icon, sort, hidden, status, remark)
SELECT 7, '删除角色', 'button', '', '', 'roles:delete', '', 4, 0, 1, ''
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_key = 'roles:delete');
INSERT INTO permissions (parent_id, name, type, route_path, component_path, permission_key, icon, sort, hidden, status, remark)
SELECT 9, '新增部门', 'button', '', '', 'departments:create', '', 1, 0, 1, ''
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_key = 'departments:create');
INSERT INTO permissions (parent_id, name, type, route_path, component_path, permission_key, icon, sort, hidden, status, remark)
SELECT 9, '编辑部门', 'button', '', '', 'departments:update', '', 2, 0, 1, ''
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_key = 'departments:update');
INSERT INTO permissions (parent_id, name, type, route_path, component_path, permission_key, icon, sort, hidden, status, remark)
SELECT 9, '删除部门', 'button', '', '', 'departments:delete', '', 3, 0, 1, ''
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_key = 'departments:delete');
INSERT INTO permissions (parent_id, name, type, route_path, component_path, permission_key, icon, sort, hidden, status, remark)
SELECT 10, '新增菜单权限', 'button', '', '', 'permissions:create', '', 1, 0, 1, ''
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_key = 'permissions:create');
INSERT INTO permissions (parent_id, name, type, route_path, component_path, permission_key, icon, sort, hidden, status, remark)
SELECT 10, '编辑菜单权限', 'button', '', '', 'permissions:update', '', 2, 0, 1, ''
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_key = 'permissions:update');
INSERT INTO permissions (parent_id, name, type, route_path, component_path, permission_key, icon, sort, hidden, status, remark)
SELECT 10, '删除菜单权限', 'button', '', '', 'permissions:delete', '', 3, 0, 1, ''
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_key = 'permissions:delete');
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, p.id FROM permissions p
WHERE p.permission_key IN ('admin-users:delete', 'roles:create', 'roles:update', 'roles:delete',
  'departments:create', 'departments:update', 'departments:delete',
  'permissions:create', 'permissions:update', 'permissions:delete')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);
