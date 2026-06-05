# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy the entire project to build the multi-module project
COPY pom.xml .
COPY commons/ ./commons/
COPY api-gateway/ ./api-gateway/
COPY core-service/ ./core-service/
COPY fraud-service/ ./fraud-service/

# Build core-service (and commons automatically)
RUN mvn clean package -pl core-service -am -DskipTests

# Download OpenTelemetry Java Agent
RUN curl -L https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar -o /app/opentelemetry-javaagent.jar

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/core-service/target/*.jar app.jar
COPY --from=builder /app/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar

EXPOSE 8081

# JAVA_TOOL_OPTIONS configures the javaagent automatically
ENV JAVA_TOOL_OPTIONS="-javaagent:/opentelemetry-javaagent.jar"

ENTRYPOINT ["java", "-jar", "app.jar"]
