package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
)

const (
	defaultEnv                    = "local"
	defaultHost                   = "0.0.0.0"
	defaultServiceName            = "risk-scoring-service"
	defaultGrpcPort               = 9091
	defaultRuleVersion            = "local-v1"
	defaultApproveMaxScore        = 49
	defaultReviewMaxScore         = 79
	defaultLogLevel               = "info"
	defaultShutdownTimeoutSeconds = 10
)

type Config struct {
	Env                    string
	Host                   string
	ServiceName            string
	GrpcPort               int
	RuleVersion            string
	ApproveMaxScore        int
	ReviewMaxScore         int
	LogLevel               string
	ShutdownTimeoutSeconds int
}

func Load() (Config, error) {
	grpcPort, err := getInt("RISK_SERVICE_GRPC_PORT", defaultGrpcPort)
	if err != nil {
		return Config{}, err
	}

	approveMaxScore, err := getInt("RISK_APPROVE_MAX_SCORE", defaultApproveMaxScore)
	if err != nil {
		return Config{}, err
	}

	reviewMaxScore, err := getInt("RISK_REVIEW_MAX_SCORE", defaultReviewMaxScore)
	if err != nil {
		return Config{}, err
	}

	shutdownTimeoutSeconds, err := getInt("SHUTDOWN_TIMEOUT_SECONDS", defaultShutdownTimeoutSeconds)
	if err != nil {
		return Config{}, err
	}

	cfg := Config{
		Env:                    getString("RISK_SERVICE_ENV", defaultEnv),
		Host:                   getString("RISK_SERVICE_HOST", defaultHost),
		ServiceName:            getString("RISK_SERVICE_NAME", defaultServiceName),
		GrpcPort:               grpcPort,
		RuleVersion:            getString("RISK_RULE_VERSION", defaultRuleVersion),
		ApproveMaxScore:        approveMaxScore,
		ReviewMaxScore:         reviewMaxScore,
		LogLevel:               strings.ToLower(getString("LOG_LEVEL", defaultLogLevel)),
		ShutdownTimeoutSeconds: shutdownTimeoutSeconds,
	}

	if err := cfg.Validate(); err != nil {
		return Config{}, err
	}

	return cfg, nil
}

func (c Config) Validate() error {
	if strings.TrimSpace(c.Env) == "" {
		return fmt.Errorf("RISK_SERVICE_ENV is required")
	}

	if strings.TrimSpace(c.Host) == "" {
		return fmt.Errorf("RISK_SERVICE_HOST is required")
	}

	if strings.TrimSpace(c.ServiceName) == "" {
		return fmt.Errorf("RISK_SERVICE_NAME is required")
	}

	if c.GrpcPort <= 0 || c.GrpcPort > 65535 {
		return fmt.Errorf("RISK_SERVICE_GRPC_PORT must be between 1 and 65535")
	}

	if strings.TrimSpace(c.RuleVersion) == "" {
		return fmt.Errorf("RISK_RULE_VERSION is required")
	}

	if c.ApproveMaxScore < 0 {
		return fmt.Errorf("RISK_APPROVE_MAX_SCORE must be >= 0")
	}

	if c.ReviewMaxScore < 0 {
		return fmt.Errorf("RISK_REVIEW_MAX_SCORE must be >= 0")
	}

	if c.ApproveMaxScore >= c.ReviewMaxScore {
		return fmt.Errorf("RISK_APPROVE_MAX_SCORE must be less than RISK_REVIEW_MAX_SCORE")
	}

	if !isSupportedLogLevel(c.LogLevel) {
		return fmt.Errorf("LOG_LEVEL must be one of debug, info, warn, or error")
	}

	if c.ShutdownTimeoutSeconds <= 0 {
		return fmt.Errorf("SHUTDOWN_TIMEOUT_SECONDS must be > 0")
	}

	return nil
}

func getString(key string, fallback string) string {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return fallback
	}

	return value
}

func getInt(key string, fallback int) (int, error) {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return fallback, nil
	}

	parsed, err := strconv.Atoi(value)
	if err != nil {
		return 0, fmt.Errorf("%s must be a valid integer: %w", key, err)
	}

	return parsed, nil
}

func isSupportedLogLevel(value string) bool {
	switch strings.ToLower(strings.TrimSpace(value)) {
	case "debug", "info", "warn", "error":
		return true
	default:
		return false
	}
}
