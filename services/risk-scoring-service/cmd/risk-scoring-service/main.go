package main

import (
	"context"
	"io"
	"log/slog"
	"os"
	"os/signal"
	"syscall"

	"github.com/k4vrin/PaymentRiskPlatform/services/risk-scoring-service/internal/config"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		slog.Error("failed to load configuration", "error", err)
		os.Exit(1)
	}

	logger := newLogger(cfg, os.Stdout)
	slog.SetDefault(logger)

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	logger.Info(
		"risk scoring service started",
		"host", cfg.Host,
		"grpc_port", cfg.GrpcPort,
		"rule_version", cfg.RuleVersion,
	)

	<-ctx.Done()

	logger.Info("risk scoring service stopped")
}

func newLogger(cfg config.Config, output io.Writer) *slog.Logger {
	level := slog.LevelInfo

	switch cfg.LogLevel {
	case "debug":
		level = slog.LevelDebug
	case "error":
		level = slog.LevelError
	case "warn":
		level = slog.LevelWarn
	case "info":
		level = slog.LevelInfo
	}

	handler := slog.NewJSONHandler(output, &slog.HandlerOptions{
		Level: level,
	})

	return slog.New(handler).With(
		"service", cfg.ServiceName,
		"env", cfg.Env,
	)
}
