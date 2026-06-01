# ── Build stage ──────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 의존성 캐시 레이어: 빌드 스크립트만 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
# Windows 체크아웃 시 gradlew가 CRLF일 수 있어 LF로 정규화 후 실행권한 부여
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

# 소스 복사 후 실행 가능한 boot jar 빌드 (테스트는 CI에서 별도 수행)
COPY src ./src
RUN ./gradlew clean bootJar --no-daemon -x test

# ── Runtime stage ────────────────────────────────────────────
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# boot jar만 복사 (plain jar는 bootJar 태스크에서 생성되지 않음)
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
