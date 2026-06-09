# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY services/payment-orchestrator-service ./services/payment-orchestrator-service
COPY proto ./proto

WORKDIR /workspace/services/payment-orchestrator-service
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home-dir /app app
COPY --from=build /workspace/services/payment-orchestrator-service/target/*.jar /app/payment-orchestrator-service.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/payment-orchestrator-service.jar"]
