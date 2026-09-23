FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM maven:3.9.9-eclipse-temurin-21

WORKDIR /app
COPY --from=build /workspace/target/admin-api-0.1.0.jar /app/admin-api.jar
RUN mkdir -p /app/storage/attachments

EXPOSE 8000
ENTRYPOINT ["java", "-jar", "/app/admin-api.jar"]
