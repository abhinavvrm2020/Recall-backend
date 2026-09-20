# syntax=docker/dockerfile:1

# --- Build ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw \
  && ./mvnw -q -B dependency:go-offline -DskipTests

COPY src src
RUN ./mvnw -q -B package -DskipTests \
  && mv target/quiz-backend-*.jar /workspace/app.jar

# --- Runtime ---
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /workspace/app.jar /app/app.jar
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh && chown -R spring:spring /app

USER spring:spring

# Railway injects PORT; default 8080 for local docker runs
ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["/app/entrypoint.sh"]
