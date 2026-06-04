package main

import (
	"context"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"net"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/k4vrin/PaymentRiskPlatform/services/risk-scoring-service/internal/config"
	riskgrpc "github.com/k4vrin/PaymentRiskPlatform/services/risk-scoring-service/internal/grpc"
	"github.com/k4vrin/PaymentRiskPlatform/services/risk-scoring-service/internal/health"
	"github.com/k4vrin/PaymentRiskPlatform/services/risk-scoring-service/internal/risk"
	googlegrpc "google.golang.org/grpc"
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

	if err := run(ctx, cfg, logger); err != nil {
		logger.Error("risk scoring service failed", "error", err)
		os.Exit(1)
	}
}

func run(ctx context.Context, cfg config.Config, logger *slog.Logger) error {
	policy, err := risk.NewDecisionPolicy(cfg.ApproveMaxScore, cfg.ReviewMaxScore)
	if err != nil {
		return fmt.Errorf("create decision policy: %w", err)
	}

	listener, err := net.Listen("tcp", fmt.Sprintf("%s:%d", cfg.Host, cfg.GrpcPort))
	if err != nil {
		return fmt.Errorf("listen on gRPC address: %w", err)
	}

	grpcServer := googlegrpc.NewServer()
	healthReporter := health.NewReporter()
	healthReporter.Register(grpcServer)

	scorer := risk.NewDefaultScorer(policy, cfg.RuleVersion)
	riskServer := riskgrpc.NewRiskScoringServer(scorer, logger)
	riskgrpc.RegisterRiskScoringServer(grpcServer, riskServer)
	healthReporter.SetServing()

	logger.Info(
		"risk scoring service started",
		"host", cfg.Host,
		"grpc_port", cfg.GrpcPort,
		"rule_version", cfg.RuleVersion,
	)

	serveErr := make(chan error, 1)
	go func() {
		serveErr <- grpcServer.Serve(listener)
	}()

	select {
	case err := <-serveErr:
		healthReporter.SetNotServing()
		if err == nil || errors.Is(err, googlegrpc.ErrServerStopped) {
			return nil
		}

		return fmt.Errorf("serve gRPC: %w", err)
	case <-ctx.Done():
	}

	logger.Info("risk scoring service shutdown started")
	healthReporter.SetNotServing()

	shutdownTimeout := time.Duration(cfg.ShutdownTimeoutSeconds) * time.Second
	gracefulStop(grpcServer, shutdownTimeout)

	logger.Info("risk scoring service stopped")
	return nil
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

type grpcServerControl interface {
	GracefulStop()
	Stop()
}

func gracefulStop(server grpcServerControl, timeout time.Duration) {
	stopped := make(chan struct{})

	go func() {
		server.GracefulStop()
		close(stopped)
	}()

	select {
	case <-stopped:
	case <-time.After(timeout):
		server.Stop()
		<-stopped
	}
}
