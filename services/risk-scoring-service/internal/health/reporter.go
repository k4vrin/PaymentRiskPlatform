package health

import (
	"google.golang.org/grpc"
	healthgrpc "google.golang.org/grpc/health"
	grpc_health_v1 "google.golang.org/grpc/health/grpc_health_v1"
)

const RiskScoringServiceName = "risk.v1.RiskScoringService"

type Reporter struct {
	server *healthgrpc.Server
}

func NewReporter() *Reporter {
	return &Reporter{
		server: healthgrpc.NewServer(),
	}
}

func (r *Reporter) Register(grpcServer grpc.ServiceRegistrar) {
	grpc_health_v1.RegisterHealthServer(grpcServer, r.server)
}

func (r *Reporter) SetServing() {
	r.server.SetServingStatus("", grpc_health_v1.HealthCheckResponse_SERVING)
	r.server.SetServingStatus(RiskScoringServiceName, grpc_health_v1.HealthCheckResponse_SERVING)
}

func (r *Reporter) SetNotServing() {
	r.server.SetServingStatus("", grpc_health_v1.HealthCheckResponse_NOT_SERVING)
	r.server.SetServingStatus(RiskScoringServiceName, grpc_health_v1.HealthCheckResponse_NOT_SERVING)
}
