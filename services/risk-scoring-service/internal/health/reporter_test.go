package health

import (
	"context"
	"testing"

	"google.golang.org/grpc"
	grpc_health_v1 "google.golang.org/grpc/health/grpc_health_v1"
)

func TestReporterRegistersHealthService(t *testing.T) {
	t.Parallel()

	grpcServer := grpc.NewServer()
	reporter := NewReporter()

	reporter.Register(grpcServer)

	serviceInfo := grpcServer.GetServiceInfo()
	if _, exists := serviceInfo["grpc.health.v1.Health"]; !exists {
		t.Fatalf("expected grpc.health.v1.Health to be registered, got %+v", serviceInfo)
	}
}

func TestReporterServingState(t *testing.T) {
	t.Parallel()

	reporter := NewReporter()
	reporter.SetServing()

	response, err := reporter.server.Check(context.Background(), &grpc_health_v1.HealthCheckRequest{
		Service: RiskScoringServiceName,
	})
	if err != nil {
		t.Fatalf("expected serving health check, got %v", err)
	}

	if response.GetStatus() != grpc_health_v1.HealthCheckResponse_SERVING {
		t.Fatalf("expected serving status, got %s", response.GetStatus())
	}

	reporter.SetNotServing()

	response, err = reporter.server.Check(context.Background(), &grpc_health_v1.HealthCheckRequest{
		Service: RiskScoringServiceName,
	})
	if err != nil {
		t.Fatalf("expected not-serving health check, got %v", err)
	}

	if response.GetStatus() != grpc_health_v1.HealthCheckResponse_NOT_SERVING {
		t.Fatalf("expected not-serving status, got %s", response.GetStatus())
	}
}
