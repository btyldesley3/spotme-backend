# Multi-stage build: compile in Maven container, run in minimal JDK image
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy entire project (pom.xml, source, etc.)
COPY . .

# Build all modules (required for multi-module project)
RUN ./mvnw clean package -DskipTests

# Runtime stage: minimal JDK + built JAR
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/app/target/app-0.1.0-SNAPSHOT.jar /app/app.jar

# Expose gRPC (9090) and REST (8080) ports
EXPOSE 9090 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

