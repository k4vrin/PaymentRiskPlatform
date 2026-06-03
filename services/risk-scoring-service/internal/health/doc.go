// Package health owns gRPC health-check behavior.
//
// Keep health reporting separate from the risk scorer so operational readiness
// can evolve independently from scoring logic.
package health