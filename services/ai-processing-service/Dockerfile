FROM eclipse-temurin:17-jdk-alpine as build

WORKDIR /build
COPY ../../gradlew .
COPY ../../gradle gradle
COPY ../../gradle.properties .
COPY ../../gradle/libs.versions.toml gradle/
COPY shared shared
COPY services/ai-processing-service services/ai-processing-service

RUN chmod +x gradlew
RUN ./gradlew :services:ai-processing-service:build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=build /build/services/ai-processing-service/build/libs/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]

