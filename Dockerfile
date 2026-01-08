# syntax=docker/dockerfile:1.6

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build

# Gradle wrapper + 설정 먼저 (캐시 효율)
COPY gradlew build.gradle settings.gradle /build/
COPY gradle /build/gradle
RUN chmod +x /build/gradlew

COPY src /build/src

RUN --mount=type=cache,target=/root/.gradle \
    /build/gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre AS prod
WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

ENV TZ=Asia/Seoul

ENTRYPOINT ["java", "-jar", "app.jar"]