# ── Stage 1: Build with Maven + Java 21 ──────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first (faster rebuilds)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Build the app
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Run with lightweight JRE ────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
