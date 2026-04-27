# 开发任务拆分

基于 PRD ([prd-admin-system.md](prd-admin-system.md)) 与技术设计 ([tech-admin-system.md](tech-admin-system.md))，将后台管理系统拆分为可执行的开发任务单元。

---

## 总览

| 阶段 | 任务数 | 后端 | 前端 | 说明 |
|------|--------|------|------|------|
| P0 工程初始化 | 3 | ✅ 已完成 | 待建 | Monorepo、Docker、前后端脚手架 |
| P1 认证模块 | 3 | ✅ 已完成 | ✅ 已完成 | 登录/登出/个人资料/修改密码 |
| P2 核心 RBAC | 8 | ✅ 已完成 | ✅ 已完成 | 管理员/角色/部门/菜单权限管理 |
| P3 日志审计 | 2 | ✅ 已完成 | ✅ 已完成 | 登录日志/操作日志 |
| P4 基础框架 | 3 | ✅ 已完成 | ✅ 已完成 | 布局/仪表盘/导航菜单 |

---

## P0 — 工程初始化

### 0.1 后端工程初始化 ✅ 已完成

**文件：** `apps/api/*`、`docker-compose.yml`

- ThinkPHP 8 项目创建与配置
- Docker Compose 编排（PHP 8.3 CLI + MySQL 8.4）
- 数据库连接配置
- JWT 依赖安装（firebase/php-jwt）
- 全局异常处理配置
- 中间件注册机制

### 0.2 数据库迁移初始化 ✅ 已完成

**文件：** `apps/api/database/migrations/*`、`apps/api/database/seeds/*`

- 通过迁移管理 10 张业务表结构
- 通过 seed 初始化根部门、超级管理员角色、权限节点、默认 admin 账号和系统配置

### 0.3 前端工程初始化 ⏸ 待建

**文件：** `apps/admin/*`（新目录）

- Vite + React 18 + TypeScript 项目创建
- Ant Design 6.x 集成
- Zustand 状态管理集成
- 路由框架（react-router）
- HTTP 客户端封装（axios）
- 环境变量配置
- pnpm workspace 注册（`pnpm-workspace.yaml` 中 `apps/*` 已声明）

---

## P1 — 认证模块

### 1.1 登录/登出 API ✅ 已完成

**文件：** `apps/api/app/controller/admin/AuthController.php`
**端点：**
- `POST /admin-api/auth/login` — 登录
- `POST /admin-api/auth/logout` — 登出

**涉及逻辑：**
- `apps/api/app/service/AuthService.php` — 登录验证（账号/密码/状态）
- `apps/api/app/middleware/JwtAuth.php` — JWT Token 鉴权
- `apps/api/app/common.php` — Token 生成/解析、密码哈希

### 1.2 个人资料/修改密码 API ✅ 已完成

**文件：** `apps/api/app/controller/admin/AuthController.php`
**端点：**
- `GET /admin-api/auth/profile` — 获取资料
- `PUT /admin-api/auth/profile` — 更新资料
- `PUT /admin-api/auth/password` — 修改密码

### 1.3 前端登录页 ⏸ 待建

**页面：** 登录页

- 账号/密码表单
- 表单必填校验
- 登录 loading 状态
- 登录失败错误提示
- 登录成功跳转首页
- Token 持久化（localStorage + Zustand store）
- 路由守卫（未登录重定向到登录页）

---

## P2 — 核心 RBAC

### 2.1 管理员管理 API ✅ 已完成

**文件：** `apps/api/app/controller/admin/AdminUserController.php`、`apps/api/app/controller/admin/MetaController.php`
**端点：**
- `GET /admin-api/admin-users` — 列表查询（MetaController）
- `POST /admin-api/admin-users` — 新增（含角色绑定）
- `PUT /admin-api/admin-users/:id` — 编辑（含角色重新绑定）
- `DELETE /admin-api/admin-users/:id` — 删除（超级管理员保护）
- `PUT /admin-api/admin-users/:id/reset-password` — 重置密码（超级管理员不可重置）

### 2.2 管理员管理前端 ⏸ 待建

**页面：** 管理员列表页、新增/编辑弹窗

- 列表展示（ID、头像、账号、昵称、部门、角色、状态、时间）
- 新增/编辑抽屉表单（账号、昵称、头像、手机号、邮箱、部门选择、多选角色、状态）
- 启用/禁用操作
- 重置密码弹窗
- 删除确认
- 超级管理员禁用删除按钮

### 2.3 角色管理 API ✅ 已完成

**文件：** `apps/api/app/controller/admin/RoleController.php`、`apps/api/app/controller/admin/MetaController.php`
**端点：**
- `GET /admin-api/roles` — 列表查询（含权限 ID）
- `POST /admin-api/roles` — 新增
- `PUT /admin-api/roles/:id` — 编辑
- `DELETE /admin-api/roles/:id` — 删除（内置角色保护、管理员占用保护）
- `GET /admin-api/permissions/tree` — 获取权限树（用于角色授权 UI）

### 2.4 角色管理前端 ⏸ 待建

**页面：** 角色列表页、授权弹窗

- 列表展示（ID、名称、编码、状态、时间）
- 新增/编辑表单（名称、编码、状态、备注）
- 权限分配：使用权限树组件勾选目录/菜单/按钮
- 删除确认（占用时提示不可删除）

### 2.5 部门管理 API ✅ 已完成

**文件：** `apps/api/app/controller/admin/DepartmentController.php`、`apps/api/app/controller/admin/MetaController.php`
**端点：**
- `GET /admin-api/departments/tree` — 部门树
- `POST /admin-api/departments` — 新增
- `PUT /admin-api/departments/:id` — 编辑
- `DELETE /admin-api/departments/:id` — 删除（根部门/子部门/成员保护）

### 2.6 部门管理前端 ⏸ 待建

**页面：** 部门管理页

- 树形展示部门结构
- 树节点上新增/编辑/删除操作
- 表单（名称、上级部门选择、负责人、电话、排序、状态）

### 2.7 菜单权限 API ✅ 已完成

**文件：** `apps/api/app/controller/admin/PermissionController.php`、`apps/api/app/controller/admin/MetaController.php`
**端点：**
- `GET /admin-api/permissions/tree` — 权限树
- `POST /admin-api/permissions` — 新增节点
- `PUT /admin-api/permissions/:id` — 编辑节点
- `DELETE /admin-api/permissions/:id` — 删除节点

**校验规则：**
- 类型层级：catalog 仅限顶级 → menu 挂 catalog 下 → button 挂 menu 下
- 内置节点（id<=16）不可删除
- 权限标识全局唯一

### 2.8 菜单权限管理前端 ⏸ 待建

**页面：** 菜单权限管理页

- 树形展示全部权限节点（目录/菜单/按钮标识）
- 节点上新增/编辑/删除操作
- 表单（名称、类型选择、父节点、路由路径、组件路径、权限标识、图标、排序、隐藏、状态）
- 类型联动：选择 catalog 时隐藏路由/组件路径

---

## P3 — 日志审计

### 3.1 登录日志 API ✅ 已完成

**文件：** `apps/api/app/controller/admin/MetaController.php`、`apps/api/app/service/AuthService.php`

- `GET /admin-api/login-logs` — 最近 50 条（AuthService 在登录时自动记录）

### 3.2 操作日志 API ✅ 已完成

**文件：** `apps/api/app/controller/admin/MetaController.php`、`apps/api/app/service/AuthService.php`

- `GET /admin-api/operation-logs` — 最近 50 条（CRUD 控制器操作时自动记录）

### 3.3 日志页面 ⏸ 待建

**页面：** 登录日志页、操作日志页

- 两页列表展示
- 如需分页，后端需补充分页参数支持

---

## P4 — 基础框架设施

### 4.1 前端布局框架 ⏸ 待建

**页面：** 后台框架

- 侧边菜单栏（根据 `/admin-api/meta/menu-tree` 动态渲染）
- 顶部导航栏（面包屑、用户信息下拉）
- 用户下拉菜单：个人资料/修改密码/退出登录
- 暗色简约主题
- 移动端响应式适配

### 4.2 仪表盘页面 ⏸ 待建

**页面：** 后台首页

- 欢迎语
- 基础统计卡片（管理员数、角色数、部门数、权限数）
- 数据来源：`GET /admin-api/meta/dashboard`

### 4.3 个人资料页面 ⏸ 待建

**页面：** 个人资料页、修改密码页

- 资料表单（头像上传、昵称、手机、邮箱）
- 修改密码表单（原密码、新密码、确认密码）

---

## 与 PRD 的差异备注

以下差异在任务拆分中已按实现为准：

- PRD 有独立的 status 更新端点，实际通过 update 端点提交 status 字段 — 遵循实现
- PRD 有独立的角色权限分配端点，实际通过 role update 的 `permission_ids` 处理 — 遵循实现
- 日志列表目前仅返回最近 50 条、无分页 — 如需分页，后端需补充支持

---

## 执行建议

1. **并行：** 前端任务中，认证模块（P1）可先做，登录页完成后即可与后端联调
2. **强依赖：** RBAC 前端页面（P2）依赖于菜单树接口，必须先确认 `/admin-api/meta/menu-tree` 可用
3. **先跑后端：** 建议先启动 Docker 后端（`pnpm run api:up`），执行数据库迁移（`pnpm run api:migrate`）和初始化数据（`pnpm run api:seed`）后，确认所有 API 正常再开始前端开发
