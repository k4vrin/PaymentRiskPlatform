package config

import "testing"

func TestLoadReturnsDefaults(t *testing.T) {
	clearEnv(t)

	cfg, err := Load()
	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}

	if cfg.Env != "local" {
		t.Fatalf("expected env local, got %s", cfg.Env)
	}

	if cfg.Host != "0.0.0.0" {
		t.Fatalf("expected host 0.0.0.0, got %s", cfg.Host)
	}

	if cfg.ServiceName != "risk-scoring-service" {
		t.Fatalf("expected service name risk-scoring-service, got %s", cfg.ServiceName)
	}

	if cfg.GrpcPort != 9091 {
		t.Fatalf("expected grpc port 9091, got %d", cfg.GrpcPort)
	}

	if cfg.RuleVersion != "local-v1" {
		t.Fatalf("expected default rule version, got %s", cfg.RuleVersion)
	}

	if cfg.ApproveMaxScore != 49 {
		t.Fatalf("expected approve max score 49, got %d", cfg.ApproveMaxScore)
	}

	if cfg.ReviewMaxScore != 79 {
		t.Fatalf("expected review max score 79, got %d", cfg.ReviewMaxScore)
	}

	if cfg.LogLevel != "info" {
		t.Fatalf("expected log level info, got %s", cfg.LogLevel)
	}

	if cfg.ShutdownTimeoutSeconds != 10 {
		t.Fatalf("expected shutdown timeout 10, got %d", cfg.ShutdownTimeoutSeconds)
	}
}

func TestLoadUsesEnvironmentOverrides(t *testing.T) {
	clearEnv(t)
	t.Setenv("RISK_SERVICE_ENV", "test")
	t.Setenv("RISK_SERVICE_HOST", "127.0.0.1")
	t.Setenv("RISK_SERVICE_NAME", "risk-test-service")
	t.Setenv("RISK_SERVICE_GRPC_PORT", "9191")
	t.Setenv("RISK_RULE_VERSION", "rules-test-v2")
	t.Setenv("RISK_APPROVE_MAX_SCORE", "40")
	t.Setenv("RISK_REVIEW_MAX_SCORE", "70")
	t.Setenv("LOG_LEVEL", "DEBUG")
	t.Setenv("SHUTDOWN_TIMEOUT_SECONDS", "5")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}

	if cfg.Env != "test" {
		t.Fatalf("expected env test, got %s", cfg.Env)
	}

	if cfg.Host != "127.0.0.1" {
		t.Fatalf("expected host override, got %s", cfg.Host)
	}

	if cfg.ServiceName != "risk-test-service" {
		t.Fatalf("expected service name override, got %s", cfg.ServiceName)
	}

	if cfg.GrpcPort != 9191 {
		t.Fatalf("expected grpc port 9191, got %d", cfg.GrpcPort)
	}

	if cfg.ApproveMaxScore != 40 {
		t.Fatalf("expected approve max score 40, got %d", cfg.ApproveMaxScore)
	}

	if cfg.ReviewMaxScore != 70 {
		t.Fatalf("expected review max score 70, got %d", cfg.ReviewMaxScore)
	}

	if cfg.RuleVersion != "rules-test-v2" {
		t.Fatalf("expected rule version override, got %s", cfg.RuleVersion)
	}

	if cfg.LogLevel != "debug" {
		t.Fatalf("expected normalized log level debug, got %s", cfg.LogLevel)
	}

	if cfg.ShutdownTimeoutSeconds != 5 {
		t.Fatalf("expected shutdown timeout 5, got %d", cfg.ShutdownTimeoutSeconds)
	}
}

func TestValidateRejectsInvalidPort(t *testing.T) {
	t.Parallel()

	cfg := Config{
		Env:                    "test",
		Host:                   "127.0.0.1",
		ServiceName:            "risk-scoring-service",
		GrpcPort:               0,
		RuleVersion:            "rules-v1",
		ApproveMaxScore:        40,
		ReviewMaxScore:         70,
		LogLevel:               "info",
		ShutdownTimeoutSeconds: 5,
	}

	if err := cfg.Validate(); err == nil {
		t.Fatal("expected validation error")
	}
}

func TestValidateRejectsInvalidThresholdOrder(t *testing.T) {
	t.Parallel()

	cfg := Config{
		Env:                    "test",
		Host:                   "127.0.0.1",
		ServiceName:            "risk-scoring-service",
		GrpcPort:               9091,
		RuleVersion:            "rules-v1",
		ApproveMaxScore:        80,
		ReviewMaxScore:         70,
		LogLevel:               "info",
		ShutdownTimeoutSeconds: 5,
	}

	if err := cfg.Validate(); err == nil {
		t.Fatal("expected validation error")
	}
}

func TestValidateRejectsInvalidLogLevel(t *testing.T) {
	t.Parallel()

	cfg := Config{
		Env:                    "test",
		Host:                   "127.0.0.1",
		ServiceName:            "risk-scoring-service",
		GrpcPort:               9091,
		RuleVersion:            "rules-v1",
		ApproveMaxScore:        40,
		ReviewMaxScore:         70,
		LogLevel:               "verbose",
		ShutdownTimeoutSeconds: 5,
	}

	if err := cfg.Validate(); err == nil {
		t.Fatal("expected validation error")
	}
}

func TestLoadRejectsInvalidIntegerOverrides(t *testing.T) {
	tests := []struct {
		name string
		key  string
	}{
		{
			name: "grpc port",
			key:  "RISK_SERVICE_GRPC_PORT",
		},
		{
			name: "approve max score",
			key:  "RISK_APPROVE_MAX_SCORE",
		},
		{
			name: "review max score",
			key:  "RISK_REVIEW_MAX_SCORE",
		},
		{
			name: "shutdown timeout",
			key:  "SHUTDOWN_TIMEOUT_SECONDS",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			clearEnv(t)
			t.Setenv(tt.key, "not-a-number")

			if _, err := Load(); err == nil {
				t.Fatal("expected load error")
			}
		})
	}
}

func clearEnv(t *testing.T) {
	t.Helper()

	t.Setenv("RISK_SERVICE_ENV", "")
	t.Setenv("RISK_SERVICE_HOST", "")
	t.Setenv("RISK_SERVICE_NAME", "")
	t.Setenv("RISK_SERVICE_GRPC_PORT", "")
	t.Setenv("RISK_RULE_VERSION", "")
	t.Setenv("RISK_APPROVE_MAX_SCORE", "")
	t.Setenv("RISK_REVIEW_MAX_SCORE", "")
	t.Setenv("LOG_LEVEL", "")
	t.Setenv("SHUTDOWN_TIMEOUT_SECONDS", "")
}
