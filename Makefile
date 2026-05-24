COMPOSE_FILE := platform/compose.local.yaml
JAVA_SERVICE_DIR := services/payment-orchestrator-service
GO_SERVICE_DIR := services/risk-scoring-service
GO_CACHE ?= /private/tmp/paymentrisk-go-build-cache
PROTO_DIR := proto
GO_PROTO_OUT := proto/gen/go
GO_BIN := $(shell go env GOPATH)/bin

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
	@echo "  make proto             Generate protobuf code"
	@echo "  make proto-go          Generate Go protobuf code"
	@echo "  make contract-test     Regenerate contracts and run Java and Go tests"
	@echo "  make test              Regenerate contracts and run Java and Go tests"
	@echo "  make java-run          Alias for make spring-run"
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
test: proto java-test go-test

.PHONY: contract-test
contract-test: proto java-test go-test

.PHONY: java-run
java-run: spring-run

.PHONY: spring-run
spring-run:
	cd $(JAVA_SERVICE_DIR) && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

.PHONY: risk-run
risk-run:
	cd $(GO_SERVICE_DIR) && GOCACHE=$(GO_CACHE) go run ./cmd/risk-scoring-service

.PHONY: proto
proto: proto-go

.PHONY: proto-go
proto-go:
	mkdir -p $(GO_PROTO_OUT)
	PATH="$(PATH):$(GO_BIN)" protoc \
		-I $(PROTO_DIR) \
		--go_out=$(GO_PROTO_OUT) \
		--go_opt=paths=source_relative \
		--go-grpc_out=$(GO_PROTO_OUT) \
		--go-grpc_opt=paths=source_relative \
		$(PROTO_DIR)/risk/v1/risk_scoring.proto
