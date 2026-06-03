// Package risk contains the core scoring domain.
//
// This package should own:
//   - scoring request/result models
//   - scoring rules
//   - threshold decision policy
//   - score aggregation
//
// It must not depend on generated protobuf types. Keeping protobuf out of
// the core domain makes the scoring logic testable without gRPC.
package risk