# syntax=docker/dockerfile:1.6

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build

COPY gradlew build.gradle settings.gradle /build/
COPY gradle /build/gradle
RUN chmod +x /build/gradlew

COPY src /build/src

RUN --mount=type=cache,target=/root/.gradle \
    /build/gradlew bootJar -x test --no-daemon


FROM eclipse-temurin:21-jre AS prod
WORKDIR /app
ENV TZ=Asia/Seoul

# 1) 빌드 산출물 전부 /tmp로 복사
COPY --from=builder /build/build/libs/*.jar /tmp/

# 2) plain.jar 제외하고 첫 번째 jar를 app.jar로 고정
# (CI 환경에서 jar가 여러 개 생겨도 안전)
RUN set -e; \
    echo "== built jars =="; ls -al /tmp/*.jar; \
    JAR="$(ls -1 /tmp/*.jar | grep -v plain | head -n 1)"; \
    echo "== selected jar =="; echo "$JAR"; \
    cp "$JAR" /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]