package grpc

import (
	"bytes"
	"context"
	"io"
	"log/slog"
	"strings"
	"testing"

	riskv1 "github.com/k4vrin/PaymentRiskPlatform/proto/gen/go/risk/v1"

	"github.com/k4vrin/PaymentRiskPlatform/services/risk-scoring-service/internal/risk"
	googlegrpc "google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

func TestRiskScoringServerScorePaymentLowRisk(t *testing.T) {
	t.Parallel()

	server := newTestRiskScoringServer(t)

	response, err := server.ScorePayment(context.Background(), &riskv1.ScorePaymentRequest{
		PaymentId:         "pay_123",
		MerchantId:        "merchant_123",
		CustomerId:        "customer_123",
		AmountMinor:       1000,
		Currency:          "USD",
		DeviceFingerprint: "device_123",
		CorrelationId:     "corr_123",
	})

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}

	if response.GetScore() != 0 {
		t.Fatalf("expected score 0, got %d", response.GetScore())
	}

	if response.GetDecision() != riskv1.RiskDecision_RISK_DECISION_APPROVED {
		t.Fatalf("expected approved decision, got %s", response.GetDecision())
	}

	if len(response.GetReasonCodes()) != 1 {
		t.Fatalf("expected 1 reason code, got %d", len(response.GetReasonCodes()))
	}

	if response.GetReasonCodes()[0] != riskv1.RiskReasonCode_RISK_REASON_CODE_LOW_RISK_PAYMENT {
		t.Fatalf("expected low risk reason code, got %s", response.GetReasonCodes()[0])
	}

	if len(response.GetRuleHits()) != 1 {
		t.Fatalf("expected 1 rule hit, got %d", len(response.GetRuleHits()))
	}

	if response.GetRuleHits()[0].GetRuleId() != risk.LowRiskRuleID {
		t.Fatalf("expected low risk rule hit, got %s", response.GetRuleHits()[0].GetRuleId())
	}

	if response.GetRuleVersion() != "local-v1" {
		t.Fatalf("expected rule version local-v1, got %s", response.GetRuleVersion())
	}
}

func TestRiskScoringServerScorePaymentHighRisk(t *testing.T) {
	t.Parallel()

	server := newTestRiskScoringServer(t)

	response, err := server.ScorePayment(context.Background(), &riskv1.ScorePaymentRequest{
		PaymentId:         "pay_123",
		MerchantId:        "high_risk_merchant_123",
		CustomerId:        "customer_123",
		AmountMinor:       risk.HighAmountThresholdMinor + 1,
		Currency:          "XXX",
		DeviceFingerprint: "repeat_device_123",
		CorrelationId:     "corr_123",
	})

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}

	expectedScore := int32(
		risk.HighAmountScoreDelta +
			risk.SuspiciousCurrencyScoreDelta +
			risk.RepeatedDeviceScoreDelta +
			risk.MerchantRiskThresholdScoreDelta,
	)

	if response.GetScore() != expectedScore {
		t.Fatalf("expected score %d, got %d", expectedScore, response.GetScore())
	}

	if response.GetDecision() != riskv1.RiskDecision_RISK_DECISION_DECLINED {
		t.Fatalf("expected declined decision, got %s", response.GetDecision())
	}

	if len(response.GetRuleHits()) != 4 {
		t.Fatalf("expected 4 rule hits, got %d", len(response.GetRuleHits()))
	}

	expectedRuleIDs := []string{
		risk.HighAmountRuleID,
		risk.SuspiciousCurrencyRuleID,
		risk.RepeatedDeviceRuleID,
		risk.MerchantRiskThresholdRuleID,
	}

	for i, expectedRuleID := range expectedRuleIDs {
		if response.GetRuleHits()[i].GetRuleId() != expectedRuleID {
			t.Fatalf(
				"expected rule hit %d to be %s, got %s",
				i,
				expectedRuleID,
				response.GetRuleHits()[i].GetRuleId(),
			)
		}
	}
}

func TestRiskScoringServerScorePaymentInvalidRequest(t *testing.T) {
	t.Parallel()

	server := newTestRiskScoringServer(t)

	response, err := server.ScorePayment(context.Background(), &riskv1.ScorePaymentRequest{
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
		t.Fatalf("expected gRPC code %s, got %s", codes.InvalidArgument, status.Code(err))
	}
}

func TestRiskScoringServerLogsCorrelationIDWhenPresent(t *testing.T) {
	t.Parallel()

	var output bytes.Buffer
	logger := slog.New(slog.NewJSONHandler(&output, nil))
	server := newTestRiskScoringServerWithLogger(t, logger)

	_, err := server.ScorePayment(context.Background(), &riskv1.ScorePaymentRequest{
		PaymentId:         "pay_123",
		MerchantId:        "merchant_123",
		CustomerId:        "customer_123",
		AmountMinor:       1000,
		Currency:          "USD",
		DeviceFingerprint: "device_123",
		CorrelationId:     "corr_123",
	})
	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}

	logLine := output.String()
	if !strings.Contains(logLine, `"correlation_id":"corr_123"`) {
		t.Fatalf("expected correlation id in log line, got %s", logLine)
	}
}

func TestRiskScoringServerDoesNotRequireCorrelationID(t *testing.T) {
	t.Parallel()

	server := newTestRiskScoringServer(t)

	response, err := server.ScorePayment(context.Background(), &riskv1.ScorePaymentRequest{
		PaymentId:         "pay_123",
		MerchantId:        "merchant_123",
		CustomerId:        "customer_123",
		AmountMinor:       1000,
		Currency:          "USD",
		DeviceFingerprint: "device_123",
	})
	if err != nil {
		t.Fatalf("expected no error without correlation id, got %v", err)
	}

	if response.GetDecision() != riskv1.RiskDecision_RISK_DECISION_APPROVED {
		t.Fatalf("expected approved decision, got %s", response.GetDecision())
	}
}

func TestRiskScoringServerDoesNotScoreInvalidRequest(t *testing.T) {
	t.Parallel()

	scorer := &spyScorer{}
	server := NewRiskScoringServer(scorer, nil)

	response, err := server.ScorePayment(context.Background(), &riskv1.ScorePaymentRequest{
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

	if scorer.called {
		t.Fatal("expected invalid request to fail before scoring")
	}
}

func TestRegisterRiskScoringServer(t *testing.T) {
	t.Parallel()

	grpcServer := googlegrpc.NewServer()
	server := newTestRiskScoringServer(t)

	RegisterRiskScoringServer(grpcServer, server)

	serviceInfo := grpcServer.GetServiceInfo()
	if _, exists := serviceInfo["risk.v1.RiskScoringService"]; !exists {
		t.Fatalf("expected risk.v1.RiskScoringService to be registered, got %+v", serviceInfo)
	}
}

func newTestRiskScoringServer(t *testing.T) *RiskScoringServer {
	t.Helper()

	logger := slog.New(slog.NewTextHandler(io.Discard, nil))

	return newTestRiskScoringServerWithLogger(t, logger)
}

func newTestRiskScoringServerWithLogger(t *testing.T, logger *slog.Logger) *RiskScoringServer {
	t.Helper()

	policy, err := risk.NewDecisionPolicy(49, 79)
	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}

	scorer := risk.NewDefaultScorer(policy, "local-v1")

	return NewRiskScoringServer(scorer, logger)
}

type spyScorer struct {
	called bool
}

func (s *spyScorer) Score(_ risk.ScoringRequest) risk.ScoringResult {
	s.called = true

	return risk.NewScoringResult(
		0,
		risk.DecisionApproved,
		[]risk.ReasonCode{risk.ReasonCodeLowRiskPayment},
		[]risk.RuleHit{
			risk.NewRuleHit(
				risk.LowRiskRuleID,
				risk.ReasonCodeLowRiskPayment,
				risk.LowRiskScoreDelta,
				"no positive-risk rules matched",
			),
		},
		"local-v1",
	)
}
