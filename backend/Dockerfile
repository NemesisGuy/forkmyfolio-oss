# ---------- STAGE 1: Build the application ----------
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cached if unchanged)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Package the application (skip tests)
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# ---------- STAGE 2: Run the application ----------
# Pinned to the 21.0.13.1 build: fixes CVE-2026-114938 (HotSpot Base64 decoder,
# OpenJDK 21 < 21.0.13.1). Revisit when the next CPU is published.
FROM eclipse-temurin:21.0.13.1_2-jre-alpine

# Set working directory
WORKDIR /app

# Copy the JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
