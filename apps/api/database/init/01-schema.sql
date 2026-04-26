SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS departments (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
  name VARCHAR(100) NOT NULL,
  leader VARCHAR(100) DEFAULT '',
  phone VARCHAR(32) DEFAULT '',
  sort INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT UNSIGNED DEFAULT NULL,
  updated_by BIGINT UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(100) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT UNSIGNED DEFAULT NULL,
  updated_by BIGINT UNSIGNED DEFAULT NULL,
  UNIQUE KEY uk_roles_name (name),
  UNIQUE KEY uk_roles_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS permissions (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
  name VARCHAR(100) NOT NULL,
  type VARCHAR(20) NOT NULL,
  route_path VARCHAR(255) DEFAULT '',
  component_path VARCHAR(255) DEFAULT '',
  permission_key VARCHAR(120) NOT NULL,
  icon VARCHAR(100) DEFAULT '',
  sort INT NOT NULL DEFAULT 0,
  hidden TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT UNSIGNED DEFAULT NULL,
  updated_by BIGINT UNSIGNED DEFAULT NULL,
  UNIQUE KEY uk_permissions_key (permission_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_users (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  department_id BIGINT UNSIGNED NOT NULL,
  username VARCHAR(100) NOT NULL,
  nickname VARCHAR(100) NOT NULL,
  avatar VARCHAR(255) DEFAULT '',
  phone VARCHAR(32) DEFAULT '',
  email VARCHAR(120) DEFAULT '',
  password_hash CHAR(32) NOT NULL,
  salt VARCHAR(32) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  is_super TINYINT NOT NULL DEFAULT 0,
  remark VARCHAR(255) DEFAULT '',
  last_login_at DATETIME DEFAULT NULL,
  last_login_ip VARCHAR(64) DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT UNSIGNED DEFAULT NULL,
  updated_by BIGINT UNSIGNED DEFAULT NULL,
  UNIQUE KEY uk_admin_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_user_roles (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  admin_user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY uk_admin_user_role (admin_user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role_permissions (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT UNSIGNED NOT NULL,
  permission_id BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS login_logs (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  admin_user_id BIGINT UNSIGNED DEFAULT NULL,
  username_snapshot VARCHAR(100) NOT NULL,
  ip VARCHAR(64) DEFAULT '',
  user_agent VARCHAR(255) DEFAULT '',
  status TINYINT NOT NULL DEFAULT 1,
  fail_reason VARCHAR(255) DEFAULT '',
  login_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS operation_logs (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  admin_user_id BIGINT UNSIGNED DEFAULT NULL,
  module VARCHAR(100) NOT NULL,
  action VARCHAR(100) NOT NULL,
  method VARCHAR(20) NOT NULL,
  path VARCHAR(255) NOT NULL,
  request_summary TEXT,
  response_summary TEXT,
  ip VARCHAR(64) DEFAULT '',
  operated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO departments (id, parent_id, name, leader, phone, sort, status, remark)
VALUES (1, 0, '总部', '', '', 0, 1, '默认根部门')
ON DUPLICATE KEY UPDATE name = VALUES(name), remark = VALUES(remark);

INSERT INTO roles (id, name, code, status, remark)
VALUES (1, '超级管理员', 'super_admin', 1, '系统内置超级管理员角色')
ON DUPLICATE KEY UPDATE name = VALUES(name), remark = VALUES(remark);

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
  (16, 14, '操作日志', 'menu', '/logs/operation', 'logs/operation', 'operation-logs:view', '', 2, 0, 1, '')
ON DUPLICATE KEY UPDATE name = VALUES(name), icon = VALUES(icon);

INSERT INTO admin_users (
  id, department_id, username, nickname, avatar, phone, email, password_hash, salt, status, is_super, remark
) VALUES (
  1, 1, 'admin', '超级管理员', '', '13800000000', 'admin@example.com', 'be952ae5498a25413eae74b5bbd4b7fa', '9f2f5c0d', 1, 1, '默认管理员，初始密码 123456'
)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), remark = VALUES(remark), password_hash = VALUES(password_hash), salt = VALUES(salt), is_super = VALUES(is_super);

INSERT INTO admin_user_roles (id, admin_user_id, role_id)
VALUES (1, 1, 1)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
