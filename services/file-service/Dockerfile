FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY ../../gradle gradle
COPY ../../gradlew .
COPY ../../build.gradle.kts .
COPY ../../settings.gradle.kts .
COPY ../../gradle.properties .
COPY ../../gradle/libs.versions.toml gradle/

COPY shared shared
COPY services/file-service services/file-service

RUN chmod +x gradlew
RUN ./gradlew :services:file-service:bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

# Create upload directory
RUN mkdir -p /app/tus-uploads && chown -R spring:spring /app/tus-uploads

USER spring:spring

COPY --from=build /app/services/file-service/build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:+UseSerialGC -Xss512k -XX:MaxRAM=384m"
ENV TUS_STORAGE_PATH=/app/tus-uploads

EXPOSE 8083

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
