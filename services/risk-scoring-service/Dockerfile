# syntax=docker/dockerfile:1

FROM golang:1.26.3-alpine AS build
WORKDIR /workspace

COPY proto ./proto
COPY services/risk-scoring-service ./services/risk-scoring-service

WORKDIR /workspace/services/risk-scoring-service
RUN go mod download
RUN go build -o /out/risk-scoring-service ./cmd/risk-scoring-service

FROM alpine:3.23
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /out/risk-scoring-service /app/risk-scoring-service

USER app
EXPOSE 9091
ENTRYPOINT ["/app/risk-scoring-service"]
