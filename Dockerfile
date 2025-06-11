# ---- Build Stage ----
FROM gradle:8.5.0-jdk17 AS builder

WORKDIR /app

# Copy everything (excluding what's in .dockerignore)
COPY . .

# Build the application
RUN gradle build --no-daemon

# ---- Runtime Stage ----
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the jar from the build stage
COPY --from=builder /app/build/libs/autoevaluator-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
