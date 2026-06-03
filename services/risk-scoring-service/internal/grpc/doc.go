// Package grpc adapts generated protobuf contracts to the internal risk scorer.
//
// This package should own:
//   - protobuf request validation
//   - protobuf-to-domain mapping
//   - domain-to-protobuf mapping
//   - RiskScoringService gRPC handler implementation
//
// Do not place scoring rules here. Handlers should delegate to internal/risk.
package grpc