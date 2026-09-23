# Admin API

Spring Boot 后台接口工程。

## 本地命令

```bash
./mvnw test
./mvnw -DskipTests package
```

当前机器如果没有本地 JDK/Maven，`./mvnw` 会使用 Docker Maven 镜像执行。

## Docker

```bash
pnpm run api:build
pnpm run api:up
pnpm run api:logs
```
