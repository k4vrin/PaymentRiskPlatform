package main

import (
	"context"
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

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	slog.Info(
		"risk scoring service started",
		"env", cfg.Env,
		"host", cfg.Host,
		"grpcPort", cfg.GrpcPort,
		"ruleVersion", cfg.RuleVersion,
	)

	<-ctx.Done()

	slog.Info("risk scoring service stopped")
}
