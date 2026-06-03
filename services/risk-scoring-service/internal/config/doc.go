// Package config owns environment parsing, defaults, and validation.
//
// Keep all configuration loading here so main.go only composes the service.
// Risk rules, gRPC handlers, and health checks should receive typed config
// values instead of reading environment variables directly.
package config