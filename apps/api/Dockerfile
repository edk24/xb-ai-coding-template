FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre

# 统一时区与 JVM 内存参数（容器内按 75% 上限使用内存）
ENV TZ=Asia/Shanghai \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Shanghai"

WORKDIR /app

# 非 root 运行：JRE 镜像默认只有 root，创建 app 用户并保证附件目录可写
RUN groupadd --system app \
    && useradd --gid app --uid 10001 --home-dir /app --shell /bin/bash app \
    && mkdir -p /app/storage/attachments \
    && chown -R app:app /app

COPY --from=build /workspace/target/admin-api-0.1.0.jar /app/admin-api.jar
RUN chown app:app /app/admin-api.jar

USER app

EXPOSE 8000

# 与 docker-compose.yml 中的健康检查保持一致：镜像内无 curl，用 bash TCP 探测
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8000'

ENTRYPOINT ["java", "-jar", "/app/admin-api.jar"]
