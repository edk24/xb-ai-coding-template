# 安全加固说明（P0 / P1）

本次改动对认证、密码、权限、附件访问与部署配置做了系统性加固。本文档记录**变更内容、行为契约与老库升级步骤**，配套数据库迁移为 `apps/api/src/main/resources/db/migration/V3__security_hardening.sql`。

## 1. 变更总览

| 编号（对应排查报告） | 变更 | 关键点 |
|------|------|--------|
| P0-1 | 接口新增**方法级权限校验** `@RequirePermission` | 只校验"已登录"不够，写接口必须校验权限码；超管短路放行；前端同步补路由与按钮守卫 |
| P0-2 | `JWT_SECRET` **启动强校验** | 必须 ≥32 字符且不能是默认值，否则应用启动失败 |
| P0-3 | 内置 admin 初始密码 `123456`，首次登录**强制改密** | 新增 `must_change_password` 字段、弱默认密码兜底与前端强制跳转 |
| P0-4 | 重置密码收紧 | 需 `admin-users:reset-password` 权限；**不再回显密码**、不再回落默认 123456；重置后强制改密并踢掉旧会话 |
| P0-5 | 配置项（COS/OSS 密钥等）**脱敏返回** | 接口不再回传云端密钥明文；保存时留空表示不覆盖 |
| P0-6 | 密码改为**明文提交 + PBKDF2 存储** | 前端删除客户端 MD5；旧 MD5 密码在登录成功后自动升级；新增登录限流 |
| P1-7 | 登出/改密/重置密码可**撤销旧 token** | 新增 `token_version` 并通过 JWT `ver` 校验，登出不再只是清 Cookie |
| P1-8 | 移除 `/storage/**` 本地附件**静态目录**访问 | 改为受鉴权接口 `GET /admin-api/attachments/{id}/file` |
| P1-9 | 附件**防 IDOR** | 下载按"超管 / `attachments:view` / 文件归属本人"判定；管理接口按权限码控制，`record` 严格校验 |
| P1-10 | 附件 URL 输出**防 XSS / 协议白名单** | 后端校验 url 协议、MIME 按扩展名收敛、`nosniff`；前端不再把存储路径当 URL 直接使用 |
| P1-11 | 审计日志**独立事务**、异常**不回传内部信息**、补充服务端日志 | 业务事务回滚不影响审计记录；统一返回安全错误文案并记录堆栈 |
| P1-12/13 | 前端配套：Cookie 会话、会话恢复、401 处理、强制改密、按钮级权限 | 刷新后通过 `/auth/profile` 恢复登录态，token 不再落 localStorage |
| P1-14/15 | 部署与工程化：compose 密钥环境变量化、非 root 镜像、CI、Flyway baseline 关闭 | 见本文与 CI 配置 |

---

## 2. 认证与会话

### 2.1 Cookie 会话

- 登录成功后，服务端在响应头写入 `Set-Cookie: ADMIN_TOKEN=<jwt>; Path=/; HttpOnly; SameSite=Lax`，有效期与 `JWT_EXPIRE_SECONDS` 一致。
- `Secure` 按请求判定：`request.isSecure()` 为真，或反向代理传来的 `X-Forwarded-Proto: https`；生产环境必须使用 HTTPS。
- 前端 `axios` 统一开启 `withCredentials: true`；**不再**从 `localStorage` 读写 token，也不再手动加 `Authorization` 头。
- 前后端需同源部署（或由同一网关转发），否则 Cookie 的 `SameSite=Lax` 与跨域策略会拦截会话。
- 过渡兼容：服务端解析顺序为 `Authorization: Bearer` → Cookie（`ADMIN_TOKEN`），前端已全面改用 Cookie。

### 2.2 会话失效（token_version）

- `admin_users.token_version` 通过 JWT 的 `ver` 声明参与签发与校验：过滤器解析 token 后与库中当前值比对，不一致直接 401。
- 以下操作会递增 `token_version`，使**已签发的旧 token 立即失效**：登出、修改密码、管理员重置他人密码。
- 因此"退出登录"是服务端失效，而不是仅清除浏览器 Cookie。

### 2.3 首次登录强制改密

- V3 迁移新增 `admin_users.must_change_password`（默认 `0`），并把内置 `admin`（id=1）置 `1`；其初始密码为 `123456`。
- 登录响应返回该标志；为 `1` 时前端只能进入修改密码页。服务端同时兜底：`123456`、`admin123`、`password` 等弱默认密码登录成功时自动置位。
- 强制改密期间仅放行 `GET/PUT /admin-api/auth/profile`、`PUT /admin-api/auth/password`、`POST /admin-api/auth/logout`，其余接口返回 **403**"请先修改初始密码"。
- 改密成功后标志清 `0`、`token_version` 递增，要求重新登录；管理员重置密码也可重新置位。

### 2.4 登录限流

- 限流 key = 小写用户名 + 客户端 IP；**15 分钟内失败 5 次**即锁定 **15 分钟**，期间登录返回 **429**"登录失败次数过多"。
- 登录成功或锁定到期后计数清零；限流在密码校验前判断。
- 当前为单实例内存实现（`LoginThrottleService`），多实例部署时限流不共享，需改用 Redis 等集中存储。

---

## 3. 密码

| 项目 | 规则 |
|------|------|
| 传输 | 前端提交**明文密码**（JSON body），不再做客户端 MD5；必须走 HTTPS |
| 存储 | PBKDF2-HMAC-SHA256，**210000 次迭代**，每用户 **16 字节随机盐**（32 位十六进制）；`password_hash` 与 `salt` 分列存储 |
| 密码策略 | **8–64 位，且同时包含字母和数字**（登录不限，改密/重置密码时服务端校验） |
| 旧密码兼容 | 旧数据 `salt` 为 8 位十六进制（4 字节）即判定为 MD5 方案；登录校验通过后**自动重算为 PBKDF2 并写回**，用户无感知 |
| 修改密码 | 校验原密码、新密码与确认密码一致；成功后旧会话失效（`token_version` 递增） |
| 初始密码 | 内置 `admin` 初始密码 `123456`，首次登录强制修改（见 2.3） |
| 管理端重置 | `PUT /admin-api/admin-users/{id}/reset-password` 需 `admin-users:reset-password` 权限，并强制下次登录改密 |

> 升级后未登录过的老账号仍保存旧 MD5 哈希，属预期状态：其下一次成功登录即完成升级。建议升级后尽快让存量账号登录一次。

---

## 4. 权限

服务端通过 `@RequirePermission("<权限码>")` 做**方法级**校验（`PermissionInterceptor`，认证通过之后执行）：

- 当前账号为超级管理员 → 直接放行（避免新增权限码后超管被锁在门外）。
- 普通账号 → 权限码必须在其 `permission_keys` 中，否则返回 **403**。
- 未登录 → **401**；前端按钮/菜单级权限只是体验优化，**不作为安全边界**。

新增权限码（V3 迁移写入 `permissions` 表）及对应接口：

| 接口 | 权限码 | 备注 |
|------|--------|------|
| `POST /admin-api/admin-users` | `admin-users:create` | |
| `PUT /admin-api/admin-users/{id}` | `admin-users:update` | |
| `DELETE /admin-api/admin-users/{id}` | `admin-users:delete` | **本次新增** |
| `PUT /admin-api/admin-users/{id}/reset-password` | `admin-users:reset-password` | |
| `POST /admin-api/roles` | `roles:create` | **本次新增** |
| `PUT /admin-api/roles/{id}` | `roles:update` | **本次新增**（含分配权限） |
| `DELETE /admin-api/roles/{id}` | `roles:delete` | **本次新增** |
| `POST /admin-api/departments` | `departments:create` | **本次新增** |
| `PUT /admin-api/departments/{id}` | `departments:update` | **本次新增** |
| `DELETE /admin-api/departments/{id}` | `departments:delete` | **本次新增** |
| `POST /admin-api/permissions` | `permissions:create` | **本次新增** |
| `PUT /admin-api/permissions/{id}` | `permissions:update` | **本次新增** |
| `DELETE /admin-api/permissions/{id}` | `permissions:delete` | **本次新增** |
| `POST /admin-api/attachments/upload` | 登录即可 | 头像等个人场景，仅做大小/类型校验 |
| `POST /admin-api/attachments/credentials`、`/record` | `attachments:upload` | |
| `DELETE /admin-api/attachments/{id}` | `attachments:delete` | |
| `GET /admin-api/attachments`、`/config` | `attachments:view` | |
| `GET /admin-api/attachments/{id}/file` | 登录即可 | 服务内再按"归属本人 / `attachments:view` / 超管"判定 |
| `GET /admin-api/admin-users`、`/roles`、`/departments/tree`、`/permissions/tree` | `admin-users:view`、`roles:view`、`departments:view`、`permissions:view` | `MetaController` 查询接口 |
| `GET /admin-api/config/items`、`/login-logs`、`/operation-logs` | `config:view`、`login-logs:view`、`operation-logs:view` | |
| `/admin-api/scheduled-jobs*`（GET/POST/PUT/DELETE） | `scheduled-jobs:view/create/update/delete/run` | 手动执行用 `scheduled-jobs:run` |

> 完整的权限码清单以 `V3__security_hardening.sql` 与各 Controller 上的 `@RequirePermission` 为准；新增接口务必同步补权限码，否则普通账号会被 403。

---

## 5. 密钥与配置

### 5.1 JWT_SECRET 启动校验

- 启动时校验 `JWT_SECRET`：**长度 ≥32 字符**且**不等于仓库中的默认开发值**，否则应用直接启动失败并提示原因（意图是让错误在部署阶段暴露，而不是带着弱密钥上线）。
- 生成方式：`openssl rand -hex 32`；不同环境使用不同密钥。
- 更换 `JWT_SECRET` 会使所有已签发 token 失效（等同全量强制重新登录），需评估窗口期。

### 5.2 敏感配置脱敏

- 配置项接口对敏感字段（key 含 `secret`、`access_key`，或以 `_password`/`_token` 结尾）**不再返回明文**：`value` 返回空字符串，另以 `has_value` 标识是否已配置。
- 保存时**敏感字段留空表示"不覆盖原值"**，避免前端掩码回写把密钥清空；非敏感字段仍按提交值更新。
- 存储密钥只应保存在数据库/环境变量中，禁止写入日志与前端 bundle。

### 5.3 审计日志与异常

- 操作日志改为**独立事务**写入：业务操作回滚时审计记录仍保留，便于追溯失败操作。
- 未处理异常统一由全局异常处理器返回**通用错误文案**，不再回传堆栈、SQL、内部类名等信息。

### 5.4 部署侧密钥注入

- `docker-compose.yml` 内不再有明文默认口令：`MYSQL_ROOT_PASSWORD`、`MYSQL_PASSWORD`、`SPRING_DATASOURCE_PASSWORD`、`JWT_SECRET` 均为 `${VAR:?请设置 ...}`，缺失时 compose 直接报错并提示变量名。
- 复制根目录 `.env.example` 为 `.env` 填写真实值（`.env` 已被 `.gitignore` 忽略）；`SPRING_DATASOURCE_PASSWORD` 需与 `MYSQL_PASSWORD` 一致。
- MySQL 端口只绑定 `127.0.0.1`，避免数据库直接暴露公网。

---

## 6. 附件访问

- **已移除** `/storage/attachments/**` 本地静态目录映射。升级后，历史数据中 `url` 为 `/storage/attachments/...` 的链接会 **404**（见第 7 节迁移）。
- 新的受鉴权下载接口：`GET /admin-api/attachments/{id}/file`
  - 本地附件：流式返回文件，仅 `image/*`（排除 svg）、`application/pdf`、`text/plain` 允许 inline，其余强制 `application/octet-stream` + 下载，并附 `X-Content-Type-Options: nosniff`。
  - COS/OSS 附件：`302` 跳转到记录中的对象存储地址（仅允许 http/https）。
  - 鉴权：登录用户 + 服务内判定（超管 / `attachments:view` / 文件归属本人），防止按自增 id 枚举他人附件。
- 列表接口已改为返回 `file_url`：本地附件固定为 `/admin-api/attachments/{id}/file`，远程附件取库中对象存储地址；前端只使用 `file_url`，不要持久化存储路径或自行拼接 URL。
- 新上传的本地附件会把 `url` 列写成空串（不再生成 `/storage/...` 地址）；**历史数据中的 `/storage/attachments/...` 不再能直接访问**，处理方式见第 7 节第 5 步。
- **COS/OSS 直传流程不变**：`POST /admin-api/attachments/credentials` 获取临时密钥 → 前端直传对象存储 → `POST /admin-api/attachments/record` 登记记录。

---

## 7. 升级步骤（老库 / 老部署）

**1）准备环境变量**

按根目录 `.env.example` 创建 `.env`，至少设置：

```bash
MYSQL_ROOT_PASSWORD=<强密码>
MYSQL_DATABASE=admin_system
MYSQL_USER=admin
MYSQL_PASSWORD=<强密码>
SPRING_DATASOURCE_PASSWORD=<与 MYSQL_PASSWORD 一致>
MYSQL_PORT=3309
JWT_SECRET=$(openssl rand -hex 32)   # 必须 ≥32 字符且非默认值
JWT_EXPIRE_SECONDS=7200
API_IMAGE_TAG=<版本号>
SPRING_PROFILES_ACTIVE=local
```

**2）备份**

```bash
docker compose exec -T mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction admin_system' > backup-$(date +%F).sql
docker run --rm -v xb-ai-coding-template_api_storage:/data -v "$PWD":/backup alpine \
  tar czf /backup/storage-$(date +%F).tgz -C /data .
```

**3）执行 V3 迁移**

- 本次已将 `spring.flyway.baseline-on-migrate` 改为 **`false`**：存量库不会被静默 baseline，结构漂移必须人工确认，避免"迁移看似成功、实际漏跑"。
- 正常老库（表结构由 V1/V2 创建、`flyway_schema_history` 完整）：直接启动应用，Flyway 自动执行 `V3__security_hardening.sql`（新增 `token_version`、`must_change_password` 字段，内置 admin 强制改密，并写入 10 个新权限码）。
- V3 的 10 个新权限码按 `permission_key` **幂等插入**（不写死主键）：`INSERT ... SELECT ... WHERE NOT EXISTS (...)`，重复执行不会报主键冲突，也不会覆盖已有自定义权限。
- 若 `flyway_schema_history` 为空、表结构是早期手工初始化的：**不要**打开 baseline 绕过，需先人工比对结构，由 Flyway baseline 到 `V1` 后执行 `migrate`；结构不一致时先在测试库验证。
- 验证：

```bash
docker compose exec mysql mysql -uadmin -p -D admin_system -e \
  "SHOW COLUMNS FROM admin_users LIKE 'token_version';
   SHOW COLUMNS FROM admin_users LIKE 'must_change_password';
   SELECT permission_key FROM permissions WHERE permission_key IN
     ('admin-users:delete','roles:create','roles:update','roles:delete',
      'departments:create','departments:update','departments:delete',
      'permissions:create','permissions:update','permissions:delete');"
```

**4）旧密码自动升级**

无需重置密码。用户下一次登录成功时，旧 MD5 值会被自动改写为 PBKDF2；未登录账号保持旧哈希，功能不受影响。

**5）处理旧附件 URL（重要）**

先备份 `attachments` 表，再**预览**受影响记录：

```sql
CREATE TABLE attachments_bak_before_hardening AS SELECT * FROM attachments;
SELECT id, storage_type, url FROM attachments
WHERE storage_type = 'local' AND url LIKE '/storage/attachments/%';
```

两种处理方式，任选其一：

```sql
-- 方式 A（推荐）：改写成新的受鉴权接口地址，浏览器自动携带 Cookie
UPDATE attachments
SET url = CONCAT('/admin-api/attachments/', id, '/file')
WHERE storage_type = 'local' AND url LIKE '/storage/attachments/%';

-- 方式 B：前端已改用 file_url（按 id 生成）时，把历史 url 置空
UPDATE attachments SET url = ''
WHERE storage_type = 'local' AND url LIKE '/storage/attachments/%';
```

- 仅处理 `storage_type = 'local'` 的记录；COS/OSS 记录的 `url` 是对象存储地址，**不要**批量改写。
- 执行前务必备份；建议先在测试库执行并抽样验证前端展示。

**6）发布与冒烟验证**

```text
□ 登录响应包含 HttpOnly Cookie ADMIN_TOKEN，前端 localStorage 中不再有 token
□ 登出后携带旧 Cookie 请求返回 401
□ admin / 123456 登录后强制跳转改密，且改密后旧会话失效
□ 无权限账号调用写接口返回 403
□ 附件列表、预览、下载正常；旧 /storage/... 链接确认已按上面 SQL 处理
□ 配置页密钥项 value 为空且有 has_value 标记，留空保存不会清空密钥
□ 故意用短 JWT_SECRET 启动 → 应用启动失败（验证强校验生效）
```

---

## 8. 安全基线 Checklist

- [ ] 生产环境启用 HTTPS，Cookie 带 `Secure`；反向代理正确传递原始协议头
- [ ] `JWT_SECRET` 使用 `openssl rand -hex 32` 生成，≥32 字符，各环境不同，未入库
- [ ] MySQL 端口仅绑定 `127.0.0.1`，数据库账号使用强密码、最小权限
- [ ] `.env` 不提交到版本库（已在 `.gitignore` 中）
- [ ] 内置 `admin` 已修改初始密码，`must_change_password` 已清零
- [ ] V3 迁移已执行，`flyway_schema_history` 无失败记录
- [ ] 历史本地附件 URL 已迁移或置空，附件下载接口验证通过
- [ ] 数据库与附件卷有备份与恢复演练
- [ ] 审计日志独立事务生效，异常响应不含内部信息
- [ ] CI 通过：前端构建 + 测试、后端测试、真实 MySQL 迁移校验
