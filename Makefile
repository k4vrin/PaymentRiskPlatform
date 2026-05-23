COMPOSE_FILE := platform/compose.local.yaml
JAVA_SERVICE_DIR := services/payment-orchestrator-service
GO_SERVICE_DIR := services/risk-scoring-service
GO_CACHE ?= /private/tmp/paymentrisk-go-build-cache

.PHONY: help
help:
	@echo "Available commands:"
	@echo "  make platform-up       Start local infrastructure"
	@echo "  make platform-down     Stop local infrastructure"
	@echo "  make platform-ps       Show local infrastructure containers"
	@echo "  make platform-logs     Follow local infrastructure logs"
	@echo "  make java-validate     Validate Spring Boot service"
	@echo "  make java-test         Run Spring Boot tests"
	@echo "  make go-test           Run Go tests"
	@echo "  make test              Run Java and Go tests"
	@echo "  make spring-run        Run Spring Boot service locally"
	@echo "  make risk-run          Run Go risk scoring service locally"

.PHONY: platform-up
platform-up:
	docker compose -f $(COMPOSE_FILE) up -d

.PHONY: platform-down
platform-down:
	docker compose -f $(COMPOSE_FILE) down

.PHONY: platform-ps
platform-ps:
	docker compose -f $(COMPOSE_FILE) ps

.PHONY: platform-logs
platform-logs:
	docker compose -f $(COMPOSE_FILE) logs -f

.PHONY: java-validate
java-validate:
	cd $(JAVA_SERVICE_DIR) && ./mvnw -DskipTests validate

.PHONY: java-test
java-test:
	cd $(JAVA_SERVICE_DIR) && ./mvnw test

.PHONY: go-test
go-test:
	cd $(GO_SERVICE_DIR) && GOCACHE=$(GO_CACHE) go test ./...

.PHONY: test
test: java-test go-test

.PHONY: spring-run
spring-run:
	cd $(JAVA_SERVICE_DIR) && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

.PHONY: risk-run
risk-run:
	cd $(GO_SERVICE_DIR) && GOCACHE=$(GO_CACHE) go run ./cmd/risk-scoring-service
