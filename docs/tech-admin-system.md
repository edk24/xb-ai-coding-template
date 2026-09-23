# 后台管理系统 — 后端技术文档

## 1. 项目概述

本项目为基于 pnpm monorepo 的中后台权限管理系统后端。首期实现统一登录认证、管理员管理、角色管理、部门管理、菜单与按钮权限管理、日志审计等核心能力。

仓库结构：

| 路径 | 说明 |
|------|------|
| `apps/api` | Java Spring Boot 后端接口工程 |
| `packages` | 共享包（预留） |
| `docs` | 需求与技术文档 |

---

## 2. 技术架构

### 2.1 架构图（文本）

```
┌─────────────────────────────────────────────────┐
│                  客户端 (Browser)                 │
│            Authorization: Bearer <JWT>            │
└──────────────────────┬──────────────────────────┘
                       │ HTTP / JSON
┌──────────────────────▼──────────────────────────┐
│              Nginx / Spring Boot API               │
│              Embedded Tomcat / Undertow            │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│              Spring MVC 路由层                    │
│              @RestController / @RequestMapping   │
│              /admin-api/* 接口分组                │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│              安全层：JWT 鉴权过滤器              │
│              security/JwtTokenFilter             │
│              Bearer Token → 解析 → 注入上下文     │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│              控制器层                             │
│  AuthController   CRUD 控制器    MetaController  │
│  (登录/登出/资料)  (管理/角色/     (只读查询/树)    │
│                    部门/权限)                     │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│              Service 服务层                       │
│              认证 / 权限 / 日志 / 业务规则         │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│              Mapper 数据访问层                   │
│              MyBatis Mapper / XML SQL            │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│              数据库层 MySQL 8.0                   │
│  8 张表：departments / roles / permissions       │
│  admin_users / admin_user_roles / role_permissions│
│  login_logs / operation_logs                     │
└─────────────────────────────────────────────────┘
```

### 2.2 技术栈

| 组件 | 技术选型 |
|------|---------|
| 运行环境 | Java 21，Docker |
| 框架 | Spring Boot 3.x (Spring Web / Spring Security / Spring Validation) |
| 构建工具 | Maven |
| ORM / SQL | MyBatis |
| JWT | HS256（Java JWT 库） |
| 数据库 | MySQL 8.0 |
| 容器化 | Docker Compose (JDK 21 + MySQL 8.0) |

### 2.3 请求生命周期

```
请求 → Spring MVC → JwtTokenFilter → Controller → Service → Mapper → MySQL
                                                ↓
                                          操作日志记录
```

- 登录接口：`POST /admin-api/auth/login` 不经过 JWT 中间件
- 其余所有 `/admin-api/*` 接口均需 `Authorization: Bearer <token>`
- JWT 过滤器验证 token → 查询用户 → 注入 `SecurityContext` / 当前用户上下文
- CRUD 控制器或业务服务在数据变更后调用 `LogService.recordOperationLog()` 记录操作日志

---

## 3. 环境要求与快速启动

### 3.1 前置依赖

- Docker & Docker Compose
- Java 21、Maven（本地直接运行时需要）
- pnpm（可选，用于根工程脚本）

### 3.2 快速启动

```bash
# 启动 API + MySQL 服务
pnpm run api:up

# 或直接使用 docker compose
docker compose up -d api mysql

# 查看日志
pnpm run api:logs

# 本地构建 Spring Boot 应用
cd apps/api && ./mvnw -DskipTests package

# 停止
pnpm run api:down
```

数据库表结构通过 `src/main/resources/db/migration` 管理，内置部门、角色、权限和默认管理员账号通过 `src/main/resources/db/seed` 初始化。

### 3.3 本地验证

```bash
# 健康检查
curl http://localhost:8000/

# 登录测试
curl -X POST http://localhost:8000/admin-api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

---

## 4. 目录结构

```
apps/api/
├── .env                          # 活跃环境配置
├── .example.env                  # 环境变量模板
├── pom.xml                       # Maven 依赖与构建配置
├── mvnw / mvnw.cmd               # Maven Wrapper
├── Dockerfile                    # JDK 21 / Spring Boot 镜像
│
├── src/
│   ├── main/
│   │   ├── java/com/hrs/admin/
│   │   │   ├── AdminApiApplication.java  # Spring Boot 启动入口
│   │   │   ├── common/                   # 统一响应、分页、工具类
│   │   │   ├── config/                   # Web/Security/MyBatis 配置
│   │   │   ├── controller/
│   │   │   │   └── admin/
│   │   │   │       ├── AuthController.java        # 登录/登出/个人资料/修改密码
│   │   │   │       ├── AdminUserController.java   # 管理员 CRUD
│   │   │   │       ├── RoleController.java        # 角色 CRUD
│   │   │   │       ├── DepartmentController.java  # 部门 CRUD
│   │   │   │       ├── PermissionController.java  # 菜单权限 CRUD
│   │   │   │       └── MetaController.java        # 只读查询（列表/树/日志/仪表盘）
│   │   │   ├── dto/                      # 请求 DTO
│   │   │   ├── vo/                       # 响应 VO
│   │   │   ├── entity/                   # 数据库实体
│   │   │   ├── mapper/                   # MyBatis Mapper 接口
│   │   │   ├── service/                  # 认证、权限、日志、业务服务
│   │   │   ├── security/                 # JWT 过滤器与当前用户上下文
│   │   │   └── exception/                # 全局异常与错误响应处理
│   │   │
│   │   └── resources/
│   │       ├── application.yml           # 应用配置
│   │       ├── mapper/                   # MyBatis XML SQL
│   │       ├── db/migration/             # 数据库迁移脚本
│   │       └── db/seed/                  # 初始化数据脚本
│   │
│   └── test/java/com/hrs/admin/   # 单元测试与接口测试
```

---

## 5. 数据库设计

### 5.1 ER 关系

```
departments 1──N admin_users N──M admin_user_roles M──N roles
                                                    │
                                              N──M role_permissions
                                                    │
                                              N──P permissions

admin_users   1──N login_logs      (登录日志)
admin_users   1──N operation_logs  (操作日志)
```

### 5.2 表结构

#### 5.2.1 departments（部门）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK | 主键 |
| parent_id | BIGINT UNSIGNED | 上级部门 ID，0 为根节点 |
| name | VARCHAR(100) | 部门名称 |
| leader | VARCHAR(100) | 负责人 |
| phone | VARCHAR(32) | 联系电话 |
| sort | INT | 排序值 |
| status | TINYINT | 1=启用 0=禁用 |
| remark | VARCHAR(255) | 备注 |
| created_at / updated_at | DATETIME | 时间戳 |
| created_by / updated_by | BIGINT UNSIGNED | 操作人 |

#### 5.2.2 roles（角色）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK | 主键 |
| name | VARCHAR(100) UNIQUE | 角色名称 |
| code | VARCHAR(100) UNIQUE | 角色编码 |
| status | TINYINT | 1=启用 0=禁用 |
| remark | VARCHAR(255) | 备注 |
| created_at / updated_at | DATETIME | 时间戳 |
| created_by / updated_by | BIGINT UNSIGNED | 操作人 |

#### 5.2.3 permissions（菜单权限）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK | 主键 |
| parent_id | BIGINT UNSIGNED | 父级 ID，0 为顶级 |
| name | VARCHAR(100) | 节点名称 |
| type | VARCHAR(20) | 类型：`catalog` 目录 / `menu` 菜单 / `button` 按钮 |
| route_path | VARCHAR(255) | 路由路径（菜单） |
| component_path | VARCHAR(255) | 组件路径（菜单） |
| permission_key | VARCHAR(120) UNIQUE | 权限标识，如 `admin-users:create` |
| icon | VARCHAR(100) | 图标名称 |
| sort | INT | 排序值 |
| hidden | TINYINT | 0=显示 1=隐藏 |
| status | TINYINT | 1=启用 0=禁用 |
| remark | VARCHAR(255) | 备注 |
| created_at / updated_at | DATETIME | 时间戳 |
| created_by / updated_by | BIGINT UNSIGNED | 操作人 |

**类型层级约束：**

```
catalog（目录，仅限顶级）
  └── menu（菜单，只能挂在 catalog 下）
       └── button（按钮，只能挂在 menu 下）
```

#### 5.2.4 admin_users（管理员）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK | 主键 |
| department_id | BIGINT UNSIGNED | 所属部门 ID |
| username | VARCHAR(100) UNIQUE | 登录账号 |
| nickname | VARCHAR(100) | 昵称 |
| avatar | VARCHAR(255) | 头像 URL |
| phone | VARCHAR(32) | 手机号 |
| email | VARCHAR(120) | 邮箱 |
| password_hash | CHAR(32) | MD5 密码哈希 |
| salt | VARCHAR(32) | 随机盐值 |
| status | TINYINT | 1=启用 0=禁用 |
| is_super | TINYINT | 1=超级管理员 0=普通管理员 |
| remark | VARCHAR(255) | 备注 |
| last_login_at | DATETIME | 最后登录时间 |
| last_login_ip | VARCHAR(64) | 最后登录 IP |
| created_at / updated_at | DATETIME | 时间戳 |
| created_by / updated_by | BIGINT UNSIGNED | 操作人 |

#### 5.2.5 admin_user_roles（管理员角色关联）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK | 主键 |
| admin_user_id | BIGINT UNSIGNED | 管理员 ID |
| role_id | BIGINT UNSIGNED | 角色 ID |
| UNIQUE KEY | (admin_user_id, role_id) | 唯一联合约束 |

#### 5.2.6 role_permissions（角色权限关联）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK | 主键 |
| role_id | BIGINT UNSIGNED | 角色 ID |
| permission_id | BIGINT UNSIGNED | 权限 ID |
| UNIQUE KEY | (role_id, permission_id) | 唯一联合约束 |

#### 5.2.7 login_logs（登录日志）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK | 主键 |
| admin_user_id | BIGINT UNSIGNED NULL | 管理员 ID（登录失败可能为 NULL） |
| username_snapshot | VARCHAR(100) | 登录时使用的账号 |
| ip | VARCHAR(64) | 登录 IP |
| user_agent | VARCHAR(255) | 浏览器/设备信息 |
| status | TINYINT | 1=成功 0=失败 |
| fail_reason | VARCHAR(255) | 失败原因 |
| login_at | DATETIME | 登录时间 |

#### 5.2.8 operation_logs（操作日志）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK | 主键 |
| admin_user_id | BIGINT UNSIGNED NULL | 操作人 ID |
| module | VARCHAR(100) | 操作模块（如"管理员管理"） |
| action | VARCHAR(100) | 操作类型（如"新增管理员"） |
| method | VARCHAR(20) | HTTP 方法 |
| path | VARCHAR(255) | 请求路径 |
| request_summary | TEXT | 请求参数摘要（JSON） |
| response_summary | TEXT | 响应结果摘要（JSON） |
| ip | VARCHAR(64) | 操作 IP |
| operated_at | DATETIME | 操作时间 |

### 5.3 种子数据

初始化时自动创建以下数据：

- **1 个根部门**：`总部`（id=1）
- **1 个内置角色**：`超级管理员`（code=`super_admin`，id=1）
- **16 个权限节点**：仪表盘菜单、系统管理目录、管理员/角色/部门/菜单权限管理、个人中心目录与菜单、日志审计目录与菜单及对应按钮权限
- **1 个超级管理员**：`admin / 123456`（id=1），归属总部，绑定超级管理员角色
- **角色-权限关联**：超级管理员角色拥有全部 16 个权限节点

---

## 6. API 接口文档

### 6.1 通用约定

**基础 URL：** `http://localhost:8000/admin-api`

**请求头：**

```
Content-Type: application/json
Authorization: Bearer <token>
```

**响应结构：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

| code | 含义 |
|------|------|
| 0 | 成功 |
| 1 | 业务错误（message 含具体描述） |
| 401 | 未登录或登录态失效 |
| 403 | 账号已禁用 |
| 404 | 资源不存在 |
| 422 | 参数校验失败 |

---

### 6.2 认证接口

#### POST /admin-api/auth/login

登录接口（不经过 JWT 鉴权）。

**请求参数：**

```json
{
  "username": "admin",
  "password": "123456"
}
```

**成功响应：**

```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "token": "eyJ0eXAiOiJKV1Qi...",
    "expire_in": 7200,
    "user": {
      "id": 1,
      "username": "admin",
      "nickname": "超级管理员",
      "avatar": "",
      "phone": "13800000000",
      "email": "admin@example.com",
      "remark": "默认管理员，初始密码 123456",
      "department_name": "总部",
      "roles": ["超级管理员"],
      "permission_keys": [
        "dashboard:view", "system", "admin-users:view", ...
      ],
      "is_super": true
    }
  }
}
```

**失败响应（账号不存在/密码错误）：**

```json
{
  "code": 1,
  "message": "账号或密码错误",
  "data": null
}
```

**失败响应（账号禁用）：**

```json
{
  "code": 1,
  "message": "账号已禁用",
  "data": null
}
```

#### POST /admin-api/auth/logout

**请求头：** Bearer Token

**响应：**

```json
{
  "code": 0,
  "message": "退出成功",
  "data": null
}
```

#### GET /admin-api/auth/profile

获取当前登录用户资料。

**响应：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "超级管理员",
    "avatar": "",
    "phone": "13800000000",
    "email": "admin@example.com",
    "remark": "默认管理员，初始密码 123456",
    "department_name": "总部",
    "roles": ["超级管理员"],
    "permission_keys": ["dashboard:view", "system", ...],
    "is_super": true
  }
}
```

#### PUT /admin-api/auth/profile

更新个人资料。

**请求参数：**

```json
{
  "nickname": "超级管理员",
  "avatar": "",
  "phone": "13800000000",
  "email": "admin@example.com",
  "remark": ""
}
```

**响应：**

```json
{
  "code": 0,
  "message": "资料已更新",
  "data": {
    "id": 1,
    "username": "admin",
    ...
  }
}
```

#### PUT /admin-api/auth/password

修改密码。

**请求参数：**

```json
{
  "old_password": "123456",
  "new_password": "newpwd123",
  "confirm_password": "newpwd123"
}
```

**成功响应：**

```json
{
  "code": 0,
  "message": "密码修改成功，请重新登录",
  "data": null
}
```

**失败响应（原密码错误）：**

```json
{
  "code": 1,
  "message": "原密码错误",
  "data": null
}
```

---

### 6.3 管理员接口

#### GET /admin-api/admin-users

获取管理员列表。

**响应：**

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "department_id": 1,
      "username": "admin",
      "nickname": "超级管理员",
      "avatar": "",
      "phone": "13800000000",
      "email": "admin@example.com",
      "status": 1,
      "is_super": 1,
      "remark": "默认管理员，初始密码 123456",
      "created_at": "2026-01-01 00:00:00",
      "department_name": "总部",
      "roles": ["超级管理员"],
      "role_ids": [1]
    }
  ]
}
```

#### POST /admin-api/admin-users

新增管理员。

**请求参数：**

```json
{
  "department_id": 1,
  "username": "zhangsan",
  "nickname": "张三",
  "avatar": "",
  "phone": "13900000001",
  "email": "zhangsan@example.com",
  "status": 1,
  "remark": "",
  "password": "123456",
  "role_ids": [1]
}
```

**成功响应：**

```json
{
  "code": 0,
  "message": "管理员已创建",
  "data": {
    "id": 2,
    "department_id": 1,
    "username": "zhangsan",
    ...
  }
}
```

#### PUT /admin-api/admin-users/:id

编辑管理员。

**请求参数（同新增，不含 password）：**

```json
{
  "department_id": 1,
  "username": "zhangsan",
  "nickname": "张三（已更新）",
  "status": 1,
  "role_ids": [1]
}
```

**响应：**

```json
{
  "code": 0,
  "message": "管理员已更新",
  "data": { ... }
}
```

#### DELETE /admin-api/admin-users/:id

删除管理员。超级管理员不可删除，不可删除自身。

**响应：**

```json
{
  "code": 0,
  "message": "管理员已删除",
  "data": null
}
```

#### PUT /admin-api/admin-users/:id/reset-password

重置密码。可选参数 `new_password`，不传时默认重置为 `123456`。

**请求参数：**

```json
{
  "new_password": "abc123"
}
```

**响应：**

```json
{
  "code": 0,
  "message": "密码已重置",
  "data": {
    "new_password": "abc123"
  }
}
```

---

### 6.4 角色接口

#### GET /admin-api/roles

获取角色列表（含已分配的权限 ID）。

**响应：**

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "name": "超级管理员",
      "code": "super_admin",
      "status": 1,
      "remark": "系统内置超级管理员角色",
      "created_at": "2026-01-01 00:00:00",
      "updated_at": "2026-01-01 00:00:00",
      "permission_ids": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16]
    }
  ]
}
```

#### POST /admin-api/roles

新增角色。

**请求参数：**

```json
{
  "name": "运营编辑",
  "code": "editor",
  "status": 1,
  "remark": "",
  "permission_ids": [1, 2, 3, 7, 11, 12]
}
```

**响应：**

```json
{
  "code": 0,
  "message": "角色已创建",
  "data": { ... }
}
```

#### PUT /admin-api/roles/:id

编辑角色（含权限重新分配）。

**请求参数：** 同新增。

**响应：**

```json
{
  "code": 0,
  "message": "角色已更新",
  "data": { ... }
}
```

#### DELETE /admin-api/roles/:id

删除角色。内置角色（id=1）不可删除；已绑定管理员的角色不可删除。

**响应：**

```json
{
  "code": 0,
  "message": "角色已删除",
  "data": null
}
```

---

### 6.5 部门接口

#### GET /admin-api/departments/tree

获取部门树。

**响应：**

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "parent_id": 0,
      "name": "总部",
      "leader": "",
      "phone": "",
      "sort": 0,
      "status": 1,
      "remark": "默认根部门",
      "children": [
        {
          "id": 2,
          "parent_id": 1,
          "name": "技术部",
          "children": []
        }
      ]
    }
  ]
}
```

#### POST /admin-api/departments

新增部门。

**请求参数：**

```json
{
  "parent_id": 0,
  "name": "技术部",
  "leader": "李四",
  "phone": "13800000000",
  "sort": 1,
  "status": 1,
  "remark": ""
}
```

**响应：**

```json
{
  "code": 0,
  "message": "部门已创建",
  "data": {
    "id": 2,
    "parent_id": 0,
    "name": "技术部",
    ...
  }
}
```

#### PUT /admin-api/departments/:id

编辑部门。不允许将上级设为自己。

**请求参数：** 同新增。

#### DELETE /admin-api/departments/:id

删除部门。根部门（id=1）不可删除；存在子部门或管理员归属时不可删除。

**响应：**

```json
{
  "code": 0,
  "message": "部门已删除",
  "data": null
}
```

---

### 6.6 菜单权限接口

#### GET /admin-api/permissions/tree

获取权限树（全部类型：目录/菜单/按钮）。

**响应结构示例：**

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "id": 2,
      "parent_id": 0,
      "name": "系统管理",
      "type": "catalog",
      "route_path": "/system",
      "component_path": "",
      "permission_key": "system",
      "icon": "SettingOutlined",
      "sort": 2,
      "hidden": 0,
      "status": 1,
      "children": [
        {
          "id": 3,
          "parent_id": 2,
          "name": "管理员管理",
          "type": "menu",
          "route_path": "/system/admin-users",
          "component_path": "system/admin-users",
          "permission_key": "admin-users:view",
          "children": [
            {
              "id": 4,
              "parent_id": 3,
              "name": "新增管理员",
              "type": "button",
              "permission_key": "admin-users:create",
              "children": []
            }
          ]
        }
      ]
    }
  ]
}
```

#### POST /admin-api/permissions

新增权限节点。

**请求参数：**

```json
{
  "parent_id": 2,
  "name": "数据统计",
  "type": "menu",
  "route_path": "/system/stats",
  "component_path": "system/stats",
  "permission_key": "stats:view",
  "icon": "",
  "sort": 5,
  "hidden": 0,
  "status": 1,
  "remark": ""
}
```

**类型层级规则：**

- `catalog` 只能作为顶级节点（parent_id=0）
- `menu` 只能挂在 `catalog` 下
- `button` 只能挂在 `menu` 下

#### PUT /admin-api/permissions/:id

编辑权限节点。内置权限（id=1,2,3）的权限标识不可修改。

#### DELETE /admin-api/permissions/:id

删除权限节点。内置节点（id<=16）不可删除；存在子节点时不可删除；自动清理关联的 `role_permissions` 记录。

---

### 6.7 日志接口

#### GET /admin-api/login-logs

获取最近 50 条登录日志。

**响应：**

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "admin_user_id": 1,
      "username_snapshot": "admin",
      "ip": "172.17.0.1",
      "user_agent": "Mozilla/5.0 ...",
      "status": 1,
      "fail_reason": "",
      "login_at": "2026-04-26 10:00:00"
    }
  ]
}
```

#### GET /admin-api/operation-logs

获取最近 50 条操作日志。

**响应：**

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "admin_user_id": 1,
      "module": "管理员管理",
      "action": "新增管理员",
      "method": "POST",
      "path": "admin-users",
      "request_summary": "{\"username\":\"zhangsan\"}",
      "response_summary": "{\"success\":true}",
      "ip": "172.17.0.1",
      "operated_at": "2026-04-26 10:05:00"
    }
  ]
}
```

### 6.8 元数据接口

#### GET /admin-api/meta/dashboard

仪表盘信息。

**响应：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "welcome": "欢迎回来，超级管理员",
    "stats": {
      "admin_users": 2,
      "roles": 3,
      "departments": 2,
      "permissions": 16
    }
  }
}
```

#### GET /admin-api/meta/menu-tree

获取当前管理员的菜单树（已按权限过滤）。

**说明：** 超级管理员返回全部启用的权限节点；普通管理员仅返回其角色所绑定的权限节点。

**响应：** 树形结构，同 `GET /admin-api/permissions/tree` 但已过滤。

---

## 7. 认证与权限模型

### 7.1 JWT 认证流程

```
登录请求 → AuthService.login() → 校验账号/密码/状态
  → 通过 → 生成 JWT Token (HS256, 默认2h过期)
  → 记录登录日志 → 返回 token + 用户信息

后续请求 → JwtTokenFilter → 提取 Bearer Token
  → 解析 payload (uid/username/is_super)
  → 查询数据库确认用户存在且启用
  → 写入 SecurityContext / AdminUserContext → 通过控制器
```

**Token Payload 结构：**

```json
{
  "iss": "admin-system",
  "sub": "1",
  "uid": 1,
  "username": "admin",
  "is_super": 1,
  "iat": 1700000000,
  "exp": 1700007200
}
```

### 7.2 RBAC 权限模型

```
用户 (admin_users)         权限类型层次
     │                        ┌──────────────┐
     │ hasMany                │  catalog 目录  │ ← 导航分组
     ▼                        └──────┬───────┘
admin_user_roles                     │ hasMany
     │                        ┌──────▼───────┐
     │ hasMany                │   menu 菜单    │ ← 页面访问
     ▼                        └──────┬───────┘
role_permissions                      │ hasMany
     │                        ┌──────▼───────┐
     ▼                        │  button 按钮  │ ← 操作权限
    roles ──── hasMany ───>  permissions    └──────────────┘
```

**权限解析链路：**

```
超级管理员：直接返回全部 permission_key
普通管理员：
  用户 → admin_user_roles → role_permissions → permissions
  → 收集全部 permission_key（多角色并集，去重）
```

**前端菜单过滤：**

```
请求 /admin-api/meta/menu-tree
超级管理员 → 全部 status=1 的权限节点
普通管理员 → 仅包含用户角色拥有的 permission_id 的节点
```

### 7.3 权限标识规范

权限标识采用 `模块:操作` 格式：

```
admin-users:view              # 管理员列表查看
admin-users:create            # 新增管理员
admin-users:update            # 编辑管理员
admin-users:reset-password    # 重置密码
roles:assign-permissions      # 分配角色权限
...
```

---

## 8. 安全设计

### 8.1 密码加密

```java
public static String adminPasswordHash(String password, String salt) {
    return md5Hex(md5Hex(password) + salt);
}
```

- 每个管理员独立 8 位十六进制随机盐值（`SecureRandom` 生成 4 字节后转十六进制字符串）
- 后台不保存明文密码
- 该方案为既有兼容约束，不作为现代密码学最佳实践推荐

### 8.2 超级管理员保护

| 规则 | 实现 |
|------|------|
| 不可删除 | `AdminUserService.delete()` 校验 `is_super` |
| 不可禁用 | `AdminUserService.update()` 校验 `is_super` 时不接受 `status=0` |
| 不可重置密码 | `AdminUserService.resetPassword()` 拒绝 `is_super` |
| 内置角色（id=1） | 不可删除 |
| 根部门（id=1） | 不可删除 |
| 内置权限节点（id<=16）| 不可删除，id=1,2,3 的标识不可修改 |

### 8.3 操作日志审计

关键管理操作自动记录操作日志，包括：

| 模块 | 记录动作 |
|------|---------|
| 个人资料 | 更新资料、修改密码 |
| 管理员管理 | 新增、编辑、删除、重置密码 |
| 角色管理 | 新增、编辑、删除 |
| 部门管理 | 新增、编辑、删除 |
| 菜单权限 | 新增、编辑、删除 |

### 8.4 参数校验

- 输入参数通过 Spring Validation / Jakarta Validation 校验（如 `@NotBlank`、`@Size`、`@Min`、`@Max`、`@Pattern`）
- 手机号、邮箱可选格式校验
- 关联数据存在性校验（如部门、角色、权限 ID）
- 唯一性校验（用户名、角色名、角色编码、权限标识）

---

## 9. 错误码规范

### 9.1 响应结构

```json
{
  "code": 0,
  "message": "ok",
  "data": null
}
```

### 9.2 通用错误码

| HTTP 状态码 | code | 说明 |
|------------|------|------|
| 200 | 0 | 操作成功 |
| 200 | 1 | 业务错误（message 中描述具体错误） |
| 401 | 1 | 未登录、Token 无效或已过期 |
| 401 | 1 | 账号不存在或密码错误 |
| 403 | 1 | 账号已被禁用 |
| 404 | 1 | 资源不存在 |
| 422 | 1 | 参数校验失败 |

### 9.3 常见业务错误

```
422: 账号已存在
422: 超级管理员不允许删除/禁用/重置
422: 两次输入的新密码不一致
422: 原密码错误
422: 内置角色不允许删除
422: 角色已绑定管理员，不能删除
422: 存在子部门/子节点，不能删除
422: 目录只能作为顶级节点
422: 菜单节点只能挂在目录下
422: 按钮节点只能挂在菜单下
```

---

## 10. 部署指南

### 10.1 Docker Compose 部署

```bash
# 构建并启动
docker compose up -d api mysql

# 查看 API 日志
docker compose logs -f api

# 进入 MySQL 容器
docker compose exec mysql mysql -u admin -p admin_system
```

### 10.2 环境变量说明

| 变量 | 默认值 | 说明 |
|------|--------|------|
| SERVER_PORT | 8000 | API 服务端口 |
| SPRING_PROFILES_ACTIVE | local | Spring 运行环境 |
| SPRING_DATASOURCE_URL | jdbc:mysql://mysql:3306/admin_system | 数据库连接地址 |
| SPRING_DATASOURCE_USERNAME | admin | 数据库用户 |
| SPRING_DATASOURCE_PASSWORD | admin123 | 数据库密码 |
| JWT_SECRET | (内置密钥) | JWT 签名密钥 |
| JWT_EXPIRE_SECONDS | 7200 | Token 过期时间（秒） |

### 10.3 注意事项

- 首次部署需要确保 MySQL 8.0 可连接，并在 API 启动时执行数据库迁移与初始化脚本
- 生产环境部署前务必修改 `JWT_SECRET` 为随机密钥
- 生产环境应使用 `prod` profile，并收敛日志级别与错误输出
- 建议使用 Nginx 反向代理到 Spring Boot 服务

---

## 11. PRD 差异说明

以下为 PRD 文档与实际实现的差异记录：

| PRD 描述 | 实际实现 |
|---------|---------|
| `PUT /admin-api/admin-users/:id/status` | 不独立存在，status 通过 update 端点一并提交 |
| `PUT /admin-api/roles/:id/status` | 不独立存在，status 通过 update 端点一并提交 |
| `GET /admin-api/roles/:id/permissions` | 权限数据通过 `GET /admin-api/roles` 的 `permission_ids` 字段返回 |
| `PUT /admin-api/roles/:id/permissions` | 权限分配通过 role create/update 的 `permission_ids` 参数处理 |
| `admin_users` 表中 `is_super` 字段 | PRD 未提及，实际实现中用于标识超级管理员 |
| 登录日志/操作日志分页 | PRD 未指定分页参数，当前返回最近 50 条（无分页） |
