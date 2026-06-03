package main

import (
	"bytes"
	"context"
	"log/slog"
	"strings"
	"testing"

	"github.com/k4vrin/PaymentRiskPlatform/services/risk-scoring-service/internal/config"
)

func TestNewLoggerUsesConfiguredLevel(t *testing.T) {
	tests := []struct {
		name          string
		level         string
		expectedDebug bool
		expectedInfo  bool
		expectedWarn  bool
	}{
		{
			name:          "debug enables debug and above",
			level:         "debug",
			expectedDebug: true,
			expectedInfo:  true,
			expectedWarn:  true,
		},
		{
			name:          "info disables debug",
			level:         "info",
			expectedDebug: false,
			expectedInfo:  true,
			expectedWarn:  true,
		},
		{
			name:          "warn disables debug and info",
			level:         "warn",
			expectedDebug: false,
			expectedInfo:  false,
			expectedWarn:  true,
		},
		{
			name:          "error disables lower levels",
			level:         "error",
			expectedDebug: false,
			expectedInfo:  false,
			expectedWarn:  false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			logger := newLogger(testConfig(tt.level), &bytes.Buffer{})
			handler := logger.Handler()

			if got := handler.Enabled(context.Background(), slog.LevelDebug); got != tt.expectedDebug {
				t.Fatalf("debug enabled = %t, want %t", got, tt.expectedDebug)
			}

			if got := handler.Enabled(context.Background(), slog.LevelInfo); got != tt.expectedInfo {
				t.Fatalf("info enabled = %t, want %t", got, tt.expectedInfo)
			}

			if got := handler.Enabled(context.Background(), slog.LevelWarn); got != tt.expectedWarn {
				t.Fatalf("warn enabled = %t, want %t", got, tt.expectedWarn)
			}
		})
	}
}

func TestNewLoggerIncludesServiceAndEnvironment(t *testing.T) {
	var output bytes.Buffer
	logger := newLogger(testConfig("info"), &output)

	logger.Info("risk scoring service started", "host", "127.0.0.1")

	logLine := output.String()
	if !strings.Contains(logLine, `"service":"risk-scoring-service"`) {
		t.Fatalf("expected service attribute in log line, got %s", logLine)
	}

	if !strings.Contains(logLine, `"env":"test"`) {
		t.Fatalf("expected env attribute in log line, got %s", logLine)
	}

	if !strings.Contains(logLine, `"host":"127.0.0.1"`) {
		t.Fatalf("expected host attribute in log line, got %s", logLine)
	}
}

func testConfig(logLevel string) config.Config {
	return config.Config{
		Env:                    "test",
		Host:                   "127.0.0.1",
		ServiceName:            "risk-scoring-service",
		GrpcPort:               9090,
		RuleVersion:            "rules-test-v1",
		ApproveMaxScore:        49,
		ReviewMaxScore:         79,
		LogLevel:               logLevel,
		ShutdownTimeoutSeconds: 10,
	}
}
