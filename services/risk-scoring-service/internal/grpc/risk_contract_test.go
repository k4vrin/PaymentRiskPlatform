package grpc

import (
	"testing"

	riskv1 "github.com/k4vrin/PaymentRiskPlatform/proto/gen/go/risk/v1"
)

func TestScorePaymentRequestConstruction(t *testing.T) {
	request := &riskv1.ScorePaymentRequest{
		PaymentId:         "pay_123",
		AmountMinor:       1299,
		Currency:          "USD",
		MerchantId:        "mer_123",
		CustomerId:        "cus_123",
		DeviceFingerprint: "device_123",
		CorrelationId:     "corr_123",
	}

	if request.GetPaymentId() != "pay_123" {
		t.Fatalf("payment id mismatch: %s", request.GetPaymentId())
	}
	if request.GetAmountMinor() != 1299 {
		t.Fatalf("amount mismatch: %d", request.GetAmountMinor())
	}
	if request.GetCorrelationId() != "corr_123" {
		t.Fatalf("correlation id mismatch: %s", request.GetCorrelationId())
	}
}

func TestScorePaymentResponseConstruction(t *testing.T) {
	response := &riskv1.ScorePaymentResponse{
		Score:    42,
		Decision: riskv1.RiskDecision_RISK_DECISION_APPROVED,
		ReasonCodes: []riskv1.RiskReasonCode{
			riskv1.RiskReasonCode_RISK_REASON_CODE_LOW_RISK_PAYMENT,
		},
		RuleHits: []*riskv1.RiskRuleHit{
			{
				RuleId:     "LOW_RISK_RULE",
				ReasonCode: riskv1.RiskReasonCode_RISK_REASON_CODE_LOW_RISK_PAYMENT,
				ScoreDelta: -10,
				Message:    "Payment is within low-risk thresholds.",
			},
		},
		RuleVersion: "local-v1",
	}

	if response.GetDecision() != riskv1.RiskDecision_RISK_DECISION_APPROVED {
		t.Fatalf("decision mismatch: %s", response.GetDecision())
	}
	if len(response.GetReasonCodes()) != 1 {
		t.Fatalf("expected one reason code, got %d", len(response.GetReasonCodes()))
	}
	if len(response.GetRuleHits()) != 1 {
		t.Fatalf("expected one rule hit, got %d", len(response.GetRuleHits()))
	}
}
