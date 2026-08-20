FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY ../../gradle gradle
COPY ../../gradlew .
COPY ../../build.gradle.kts .
COPY ../../settings.gradle.kts .
COPY ../../gradle.properties .
COPY ../../gradle/libs.versions.toml gradle/

COPY shared shared
COPY services/api-gateway services/api-gateway

RUN chmod +x gradlew
RUN ./gradlew :services:api-gateway:bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/services/api-gateway/build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:+UseSerialGC -Xss512k -XX:MaxRAM=256m"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
