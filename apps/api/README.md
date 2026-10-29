# Admin API

Spring Boot 后台接口工程。

## 本地命令

```bash
./mvnw test
./mvnw -DskipTests package
```

当前机器如果没有本地 JDK/Maven，`./mvnw` 会使用 Docker Maven 镜像执行。

- 执行 `./mvnw test` 需要 **JDK 21** 或 **Docker**（脚本会优先用本机 `mvn`，否则回退到 `maven:3.9.9-eclipse-temurin-21` 镜像）。
- 构建产物：`target/admin-api-0.1.0.jar`。

## 环境变量

部署与本地运行需要以下变量（模板见根目录 `.env.example`；缺失时 `docker compose` 会直接报错）：

| 变量 | 说明 |
|------|------|
| `SPRING_DATASOURCE_URL` | JDBC 连接串（compose 内由 `MYSQL_DATABASE` 拼装） |
| `SPRING_DATASOURCE_USERNAME` | 数据库账号（默认 `admin`） |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码，**必填**，需与 `MYSQL_PASSWORD` 一致 |
| `JWT_SECRET` | JWT 签名密钥，**必填**，≥32 字符，生成：`openssl rand -hex 32` |
| `JWT_EXPIRE_SECONDS` | 会话有效期（秒），默认 7200 |
| `SERVER_PORT` | API 端口，默认 8000 |
| `SPRING_PROFILES_ACTIVE` | Spring profile，默认 `local`（仓库当前无 `application-local.yml`，使用默认配置） |
| `UPLOAD_ROOT` | 本地附件根目录，默认 `/app/storage/attachments`（容器内） |
| `ATTACHMENT_MAX_SIZE` / `ATTACHMENT_ALLOWED_EXTENSIONS` | 附件大小上限与扩展名白名单 |

> `JWT_SECRET` 有启动强校验：长度 <32 或等于仓库默认开发值时应用**启动失败**，这是预期行为。

## 数据库迁移

Flyway 管理 `src/main/resources/db/migration` 下的全部迁移：

- 已关闭 `baseline-on-migrate`：存量库不会被静默 baseline，结构漂移需人工确认。
- `V3__security_hardening.sql` 为本次安全加固迁移（`token_version`、`must_change_password` 字段及新增权限码），老库升级、旧密码自动升级、旧附件 URL 处理步骤见 [docs/security-hardening.md](../../docs/security-hardening.md)。
- 迁移在 CI 中通过真实 MySQL 8.0 校验（`.github/workflows/ci.yml` 的 `migration` job）。

## Docker

```bash
pnpm run api:build
pnpm run api:up
pnpm run api:logs
```

镜像基于 `eclipse-temurin:21-jre`，以非 root 用户 `app` 运行，内置 `HEALTHCHECK`（bash TCP 探测 8000 端口），时区固定 `Asia/Shanghai`。
