# Multi-stage Dockerfile for CloudCost AI Spring Boot Backend
# Stage 1: Build JAR using Maven and Eclipse Temurin 21
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependency layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build executable fat JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Distroless JRE runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for container security
RUN addgroup -S cloudcost && adduser -S cloudcost -G cloudcost
USER cloudcost:cloudcost

# Copy compiled jar from builder
COPY --from=builder /app/target/cloudcost-ai-backend-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
