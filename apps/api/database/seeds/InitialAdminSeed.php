<?php

use think\migration\Seeder;

class InitialAdminSeed extends Seeder
{
    public function run(): void
    {
        foreach ($this->statements() as $statement) {
            $this->execute($statement);
        }
    }

    private function statements(): array
    {
        return [
            <<<'SQL'
INSERT INTO departments (id, parent_id, name, leader, phone, sort, status, remark)
VALUES (1, 0, '总部', '', '', 0, 1, '默认根部门')
ON DUPLICATE KEY UPDATE name = VALUES(name), remark = VALUES(remark)
SQL,
            <<<'SQL'
INSERT INTO roles (id, name, code, status, data_scope, remark)
VALUES (1, '超级管理员', 'super_admin', 1, 4, '系统内置超级管理员角色')
ON DUPLICATE KEY UPDATE name = VALUES(name), data_scope = VALUES(data_scope), remark = VALUES(remark)
SQL,
            <<<'SQL'
INSERT INTO permissions (id, parent_id, name, type, route_path, component_path, permission_key, icon, sort, hidden, status, remark)
VALUES
  (1, 0, '仪表盘', 'menu', '/dashboard', 'dashboard', 'dashboard:view', 'DashboardOutlined', 1, 0, 1, ''),
  (2, 0, '系统管理', 'catalog', '/system', '', 'system', 'SettingOutlined', 2, 0, 1, ''),
  (3, 2, '管理员管理', 'menu', '/system/admin-users', 'system/admin-users', 'admin-users:view', '', 1, 0, 1, ''),
  (4, 3, '新增管理员', 'button', '', '', 'admin-users:create', '', 1, 0, 1, ''),
  (5, 3, '编辑管理员', 'button', '', '', 'admin-users:update', '', 2, 0, 1, ''),
  (6, 3, '重置密码', 'button', '', '', 'admin-users:reset-password', '', 3, 0, 1, ''),
  (7, 2, '角色管理', 'menu', '/system/roles', 'system/roles', 'roles:view', '', 2, 0, 1, ''),
  (8, 7, '分配权限', 'button', '', '', 'roles:assign-permissions', '', 1, 0, 1, ''),
  (9, 2, '部门管理', 'menu', '/system/departments', 'system/departments', 'departments:view', '', 3, 0, 1, ''),
  (10, 2, '菜单权限', 'menu', '/system/permissions', 'system/permissions', 'permissions:view', '', 4, 0, 1, ''),
  (11, 0, '个人中心', 'catalog', '/profile', '', 'profile', 'UserOutlined', 3, 0, 1, ''),
  (12, 11, '个人资料', 'menu', '/profile/basic', 'profile/basic', 'profile:view', '', 1, 0, 1, ''),
  (13, 11, '修改密码', 'menu', '/profile/password', 'profile/password', 'profile:password', '', 2, 0, 1, ''),
  (14, 0, '日志审计', 'catalog', '/logs', '', 'logs', 'FileTextOutlined', 4, 0, 1, ''),
  (15, 14, '登录日志', 'menu', '/logs/login', 'logs/login', 'login-logs:view', '', 1, 0, 1, ''),
  (16, 14, '操作日志', 'menu', '/logs/operation', 'logs/operation', 'operation-logs:view', '', 2, 0, 1, ''),
  (17, 0, '附件中心', 'menu', '/attachments', 'attachments', 'attachments:view', 'PaperClipOutlined', 5, 0, 1, ''),
  (18, 17, '上传附件', 'button', '', '', 'attachments:upload', '', 1, 0, 1, ''),
  (19, 17, '删除附件', 'button', '', '', 'attachments:delete', '', 2, 0, 1, ''),
  (20, 2, '系统配置', 'menu', '/system/config', 'system/config', 'config:view', '', 5, 0, 1, ''),
  (21, 20, '新增配置', 'button', '', '', 'config:create', '', 1, 0, 1, ''),
  (22, 20, '编辑配置', 'button', '', '', 'config:update', '', 2, 0, 1, ''),
  (23, 20, '删除配置', 'button', '', '', 'config:delete', '', 3, 0, 1, '')
ON DUPLICATE KEY UPDATE name = VALUES(name), icon = VALUES(icon)
SQL,
            <<<'SQL'
INSERT INTO admin_users (
  id, department_id, username, nickname, avatar, phone, email, password_hash, salt, status, is_super, remark
) VALUES (
  1, 1, 'admin', '超级管理员', '', '13800000000', 'admin@example.com', 'be952ae5498a25413eae74b5bbd4b7fa', '9f2f5c0d', 1, 1, '默认管理员，初始密码 123456'
)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), remark = VALUES(remark), password_hash = VALUES(password_hash), salt = VALUES(salt), is_super = VALUES(is_super)
SQL,
            <<<'SQL'
INSERT INTO admin_user_roles (id, admin_user_id, role_id)
VALUES (1, 1, 1)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id)
SQL,
            <<<'SQL'
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id)
SQL,
            <<<'SQL'
INSERT INTO config_items (name, group_name, `key`, value, type, options, sort, status, remark)
VALUES
  ('存储驱动', '存储', 'storage_driver', 'local', 'select', '[{"label":"本地","value":"local"},{"label":"腾讯云COS","value":"cos"},{"label":"阿里云OSS","value":"oss"}]', 1, 1, '附件存储方式'),
  ('COS 地域', '存储', 'cos_region', '', 'input', '', 10, 1, ''),
  ('COS 存储桶', '存储', 'cos_bucket', '', 'input', '', 11, 1, ''),
  ('COS SecretId', '存储', 'cos_secret_id', '', 'input', '', 12, 1, ''),
  ('COS SecretKey', '存储', 'cos_secret_key', '', 'input', '', 13, 1, ''),
  ('COS AppId', '存储', 'cos_app_id', '', 'input', '', 14, 1, ''),
  ('COS CDN 加速域名', '存储', 'cos_cdn_url', '', 'input', '', 15, 1, ''),
  ('COS 允许上传前缀', '存储', 'cos_allow_prefix', 'uploads', 'input', '', 16, 1, ''),
  ('COS 临时密钥有效期', '存储', 'cos_duration', '1800', 'number', '', 17, 1, '单位：秒'),
  ('OSS 地域', '存储', 'oss_region', '', 'input', '', 20, 1, ''),
  ('OSS 存储桶', '存储', 'oss_bucket', '', 'input', '', 21, 1, ''),
  ('OSS AccessKeyId', '存储', 'oss_access_key_id', '', 'input', '', 22, 1, ''),
  ('OSS AccessKeySecret', '存储', 'oss_access_key_secret', '', 'input', '', 23, 1, ''),
  ('OSS RoleArn', '存储', 'oss_role_arn', '', 'input', '', 24, 1, ''),
  ('OSS Endpoint', '存储', 'oss_endpoint', '', 'input', '', 25, 1, ''),
  ('OSS CDN 加速域名', '存储', 'oss_cdn_url', '', 'input', '', 26, 1, ''),
  ('OSS 允许上传前缀', '存储', 'oss_allow_prefix', 'uploads', 'input', '', 27, 1, ''),
  ('OSS 临时密钥有效期', '存储', 'oss_duration', '1800', 'number', '', 28, 1, '单位：秒')
ON DUPLICATE KEY UPDATE name = VALUES(name), group_name = VALUES(group_name), type = VALUES(type), options = VALUES(options), sort = VALUES(sort), status = VALUES(status), remark = VALUES(remark)
SQL,
        ];
    }
}
