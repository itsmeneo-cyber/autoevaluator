# Start with an OpenJDK base image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the built jar from host to container
COPY build/libs/autoevaluator-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080 (or your Spring Boot port)
EXPOSE 8082

# Command to run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
