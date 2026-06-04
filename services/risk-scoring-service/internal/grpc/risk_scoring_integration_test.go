package grpc

import (
	"context"
	"io"
	"log/slog"
	"net"
	"testing"

	riskv1 "github.com/k4vrin/PaymentRiskPlatform/proto/gen/go/risk/v1"
	"github.com/k4vrin/PaymentRiskPlatform/services/risk-scoring-service/internal/health"
	"github.com/k4vrin/PaymentRiskPlatform/services/risk-scoring-service/internal/risk"
	googlegrpc "google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/credentials/insecure"
	grpc_health_v1 "google.golang.org/grpc/health/grpc_health_v1"
	"google.golang.org/grpc/status"
	"google.golang.org/grpc/test/bufconn"
)

const testBufferSize = 1024 * 1024

func TestRiskScoringServiceIntegration(t *testing.T) {
	t.Parallel()

	client, healthClient, stop := startTestRiskScoringService(t)
	defer stop()

	t.Run("health serving", func(t *testing.T) {
		response, err := healthClient.Check(context.Background(), &grpc_health_v1.HealthCheckRequest{
			Service: health.RiskScoringServiceName,
		})
		if err != nil {
			t.Fatalf("expected health response, got %v", err)
		}

		if response.GetStatus() != grpc_health_v1.HealthCheckResponse_SERVING {
			t.Fatalf("expected serving status, got %s", response.GetStatus())
		}
	})

	t.Run("low risk request", func(t *testing.T) {
		response, err := client.ScorePayment(context.Background(), &riskv1.ScorePaymentRequest{
			PaymentId:         "pay_low",
			AmountMinor:       1000,
			Currency:          "USD",
			MerchantId:        "merchant_123",
			CustomerId:        "customer_123",
			DeviceFingerprint: "device_123",
			CorrelationId:     "corr_low",
		})
		if err != nil {
			t.Fatalf("expected low risk response, got %v", err)
		}

		if response.GetDecision() != riskv1.RiskDecision_RISK_DECISION_APPROVED {
			t.Fatalf("expected approved decision, got %s", response.GetDecision())
		}

		if response.GetReasonCodes()[0] != riskv1.RiskReasonCode_RISK_REASON_CODE_LOW_RISK_PAYMENT {
			t.Fatalf("expected low risk reason code, got %s", response.GetReasonCodes()[0])
		}
	})

	t.Run("multi rule high risk request", func(t *testing.T) {
		response, err := client.ScorePayment(context.Background(), &riskv1.ScorePaymentRequest{
			PaymentId:         "pay_high",
			AmountMinor:       risk.HighAmountThresholdMinor + 1,
			Currency:          "XXX",
			MerchantId:        "high_risk_merchant_123",
			CustomerId:        "customer_123",
			DeviceFingerprint: "repeat_device_123",
			CorrelationId:     "corr_high",
		})
		if err != nil {
			t.Fatalf("expected high risk response, got %v", err)
		}

		if response.GetDecision() != riskv1.RiskDecision_RISK_DECISION_DECLINED {
			t.Fatalf("expected declined decision, got %s", response.GetDecision())
		}

		if len(response.GetRuleHits()) != 4 {
			t.Fatalf("expected 4 rule hits, got %d", len(response.GetRuleHits()))
		}
	})

	t.Run("invalid request", func(t *testing.T) {
		response, err := client.ScorePayment(context.Background(), &riskv1.ScorePaymentRequest{
			PaymentId:   "",
			AmountMinor: 1000,
			Currency:    "USD",
			MerchantId:  "merchant_123",
			CustomerId:  "customer_123",
		})

		if err == nil {
			t.Fatal("expected validation error, got nil")
		}

		if response != nil {
			t.Fatalf("expected nil response, got %v", response)
		}

		if status.Code(err) != codes.InvalidArgument {
			t.Fatalf("expected invalid argument status, got %s", status.Code(err))
		}
	})
}

func startTestRiskScoringService(
	t *testing.T,
) (riskv1.RiskScoringServiceClient, grpc_health_v1.HealthClient, func()) {
	t.Helper()

	listener := bufconn.Listen(testBufferSize)

	policy, err := risk.NewDecisionPolicy(49, 79)
	if err != nil {
		t.Fatalf("create decision policy: %v", err)
	}

	grpcServer := googlegrpc.NewServer()
	healthReporter := health.NewReporter()
	healthReporter.Register(grpcServer)
	RegisterRiskScoringServer(
		grpcServer,
		NewRiskScoringServer(
			risk.NewDefaultScorer(policy, "local-v1"),
			slog.New(slog.NewTextHandler(io.Discard, nil)),
		),
	)
	healthReporter.SetServing()

	go func() {
		_ = grpcServer.Serve(listener)
	}()

	conn, err := googlegrpc.NewClient(
		"passthrough:///bufnet",
		googlegrpc.WithTransportCredentials(insecure.NewCredentials()),
		googlegrpc.WithContextDialer(func(ctx context.Context, _ string) (net.Conn, error) {
			return listener.DialContext(ctx)
		}),
	)
	if err != nil {
		grpcServer.Stop()
		t.Fatalf("dial risk service: %v", err)
	}

	stop := func() {
		healthReporter.SetNotServing()
		grpcServer.Stop()
		_ = conn.Close()
	}

	return riskv1.NewRiskScoringServiceClient(conn), grpc_health_v1.NewHealthClient(conn), stop
}
