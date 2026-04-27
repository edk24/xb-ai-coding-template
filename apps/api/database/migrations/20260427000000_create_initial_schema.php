<?php

use think\migration\Migrator;

class CreateInitialSchema extends Migrator
{
    public function up(): void
    {
        foreach ($this->statements() as $statement) {
            $this->execute($statement);
        }
    }

    public function down(): void
    {
        foreach ([
            'operation_logs',
            'config_items',
            'login_logs',
            'attachments',
            'role_permissions',
            'admin_user_roles',
            'admin_users',
            'permissions',
            'roles',
            'departments',
        ] as $table) {
            $this->execute("DROP TABLE IF EXISTS {$table}");
        }
    }

    private function statements(): array
    {
        return [
            <<<'SQL'
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
SQL,
            <<<'SQL'
CREATE TABLE IF NOT EXISTS roles (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(100) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  data_scope TINYINT NOT NULL DEFAULT 2 COMMENT '数据权限：1=仅本人 2=本部门 3=本部门及以下 4=全部',
  remark VARCHAR(255) DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT UNSIGNED DEFAULT NULL,
  updated_by BIGINT UNSIGNED DEFAULT NULL,
  UNIQUE KEY uk_roles_name (name),
  UNIQUE KEY uk_roles_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
SQL,
            <<<'SQL'
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
SQL,
            <<<'SQL'
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
SQL,
            <<<'SQL'
CREATE TABLE IF NOT EXISTS admin_user_roles (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  admin_user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY uk_admin_user_role (admin_user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
SQL,
            <<<'SQL'
CREATE TABLE IF NOT EXISTS role_permissions (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT UNSIGNED NOT NULL,
  permission_id BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
SQL,
            <<<'SQL'
CREATE TABLE IF NOT EXISTS attachments (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  admin_user_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(255) NOT NULL,
  size BIGINT UNSIGNED NOT NULL,
  mime_type VARCHAR(127) DEFAULT '',
  storage_type VARCHAR(20) NOT NULL DEFAULT 'local',
  path VARCHAR(500) NOT NULL,
  url VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (admin_user_id),
  INDEX idx_type (storage_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
SQL,
            <<<'SQL'
CREATE TABLE IF NOT EXISTS login_logs (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  admin_user_id BIGINT UNSIGNED DEFAULT NULL,
  username_snapshot VARCHAR(100) NOT NULL,
  ip VARCHAR(64) DEFAULT '',
  user_agent VARCHAR(255) DEFAULT '',
  status TINYINT NOT NULL DEFAULT 1,
  fail_reason VARCHAR(255) DEFAULT '',
  login_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
SQL,
            <<<'SQL'
CREATE TABLE IF NOT EXISTS config_items (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  group_name VARCHAR(100) NOT NULL DEFAULT '',
  `key` VARCHAR(120) NOT NULL,
  value TEXT,
  type VARCHAR(30) NOT NULL DEFAULT 'input',
  options TEXT COMMENT 'JSON, for select/radio/checkbox',
  sort INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT UNSIGNED DEFAULT NULL,
  updated_by BIGINT UNSIGNED DEFAULT NULL,
  UNIQUE KEY uk_config_items_key (`key`),
  INDEX idx_group_name (group_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
SQL,
            <<<'SQL'
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
SQL,
        ];
    }
}
