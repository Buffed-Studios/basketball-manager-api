# ── Build stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Gradle wrapper and dependency manifests first for layer caching
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle settings.gradle* ./

# Pre-download dependencies (best-effort; fails gracefully if network restricted)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copy source and build the fat JAR, skipping tests (tests need a live DB)
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

