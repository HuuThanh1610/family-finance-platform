# Multi-stage build — context: services/profile-service
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build
RUN apk add --no-cache maven
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
ENV SERVER_PORT=8081
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8081
HEALTHCHECK --interval=15s --timeout=5s --retries=12 --start-period=90s \
  CMD curl -fsS "http://127.0.0.1:${SERVER_PORT}/actuator/health" | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
