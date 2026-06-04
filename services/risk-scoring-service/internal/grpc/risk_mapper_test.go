package grpc

import (
	"testing"

	riskv1 "github.com/k4vrin/PaymentRiskPlatform/proto/gen/go/risk/v1"

	"github.com/k4vrin/PaymentRiskPlatform/services/risk-scoring-service/internal/risk"
)

func TestToScoringRequest(t *testing.T) {
	t.Parallel()

	request := &riskv1.ScorePaymentRequest{
		PaymentId:         "pay_123",
		MerchantId:        "merchant_123",
		CustomerId:        "customer_123",
		AmountMinor:       1500,
		Currency:          "USD",
		DeviceFingerprint: "device_123",
		CorrelationId:     "corr_123",
	}

	result := ToScoringRequest(request)

	if result.PaymentID != "pay_123" {
		t.Fatalf("expected payment id pay_123, got %s", result.PaymentID)
	}

	if result.MerchantID != "merchant_123" {
		t.Fatalf("expected merchant id merchant_123, got %s", result.MerchantID)
	}

	if result.CustomerID != "customer_123" {
		t.Fatalf("expected customer id customer_123, got %s", result.CustomerID)
	}

	if result.AmountMinor != 1500 {
		t.Fatalf("expected amount minor 1500, got %d", result.AmountMinor)
	}

	if result.Currency != "USD" {
		t.Fatalf("expected currency USD, got %s", result.Currency)
	}

	if result.DeviceFingerprint != "device_123" {
		t.Fatalf("expected device fingerprint device_123, got %s", result.DeviceFingerprint)
	}

	if result.CorrelationID != "corr_123" {
		t.Fatalf("expected correlation id corr_123, got %s", result.CorrelationID)
	}
}

func TestToScoringRequestNilRequest(t *testing.T) {
	t.Parallel()

	result := ToScoringRequest(nil)

	if result != (risk.ScoringRequest{}) {
		t.Fatalf("expected zero scoring request, got %+v", result)
	}
}

func TestToProtoRiskDecision(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name     string
		input    risk.Decision
		expected riskv1.RiskDecision
	}{
		{
			name:     "approved",
			input:    risk.DecisionApproved,
			expected: riskv1.RiskDecision_RISK_DECISION_APPROVED,
		},
		{
			name:     "review required",
			input:    risk.DecisionReviewRequired,
			expected: riskv1.RiskDecision_RISK_DECISION_REVIEW_REQUIRED,
		},
		{
			name:     "declined",
			input:    risk.DecisionDeclined,
			expected: riskv1.RiskDecision_RISK_DECISION_DECLINED,
		},
		{
			name:     "unknown",
			input:    risk.Decision("UNKNOWN"),
			expected: riskv1.RiskDecision_RISK_DECISION_UNSPECIFIED,
		},
	}

	for _, tt := range tests {
		tt := tt

		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			actual := ToProtoRiskDecision(tt.input)

			if actual != tt.expected {
				t.Fatalf("expected %s, got %s", tt.expected, actual)
			}
		})
	}
}

func TestToProtoReasonCode(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name     string
		input    risk.ReasonCode
		expected riskv1.RiskReasonCode
	}{
		{
			name:     "low risk",
			input:    risk.ReasonCodeLowRiskPayment,
			expected: riskv1.RiskReasonCode_RISK_REASON_CODE_LOW_RISK_PAYMENT,
		},
		{
			name:     "high amount",
			input:    risk.ReasonCodeHighAmount,
			expected: riskv1.RiskReasonCode_RISK_REASON_CODE_HIGH_AMOUNT,
		},
		{
			name:     "suspicious currency",
			input:    risk.ReasonCodeSuspiciousCurrency,
			expected: riskv1.RiskReasonCode_RISK_REASON_CODE_SUSPICIOUS_CURRENCY,
		},
		{
			name:     "repeated device",
			input:    risk.ReasonCodeRepeatedDevice,
			expected: riskv1.RiskReasonCode_RISK_REASON_CODE_REPEATED_DEVICE,
		},
		{
			name:     "merchant risk threshold exceeded",
			input:    risk.ReasonCodeMerchantRiskThresholdExceeded,
			expected: riskv1.RiskReasonCode_RISK_REASON_CODE_MERCHANT_RISK_THRESHOLD_EXCEEDED,
		},
		{
			name:     "unknown",
			input:    risk.ReasonCode("UNKNOWN"),
			expected: riskv1.RiskReasonCode_RISK_REASON_CODE_UNSPECIFIED,
		},
	}

	for _, tt := range tests {
		tt := tt

		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			actual := ToProtoReasonCode(tt.input)

			if actual != tt.expected {
				t.Fatalf("expected %s, got %s", tt.expected, actual)
			}
		})
	}
}

func TestToProtoRuleHit(t *testing.T) {
	t.Parallel()

	hit := risk.NewRuleHit(
		risk.HighAmountRuleID,
		risk.ReasonCodeHighAmount,
		risk.HighAmountScoreDelta,
		"payment amount exceeds high amount threshold",
	)

	result := ToProtoRuleHit(hit)

	if result.GetRuleId() != risk.HighAmountRuleID {
		t.Fatalf("expected rule id %s, got %s", risk.HighAmountRuleID, result.GetRuleId())
	}

	if result.GetReasonCode() != riskv1.RiskReasonCode_RISK_REASON_CODE_HIGH_AMOUNT {
		t.Fatalf("expected reason code %s, got %s", riskv1.RiskReasonCode_RISK_REASON_CODE_HIGH_AMOUNT, result.GetReasonCode())
	}

	if result.GetScoreDelta() != int32(risk.HighAmountScoreDelta) {
		t.Fatalf("expected score delta %d, got %d", risk.HighAmountScoreDelta, result.GetScoreDelta())
	}

	if result.GetMessage() == "" {
		t.Fatal("expected message to be present")
	}
}

func TestToProtoScorePaymentResponse(t *testing.T) {
	t.Parallel()

	result := risk.NewScoringResult(
		35,
		risk.DecisionApproved,
		[]risk.ReasonCode{risk.ReasonCodeHighAmount},
		[]risk.RuleHit{
			risk.NewRuleHit(
				risk.HighAmountRuleID,
				risk.ReasonCodeHighAmount,
				risk.HighAmountScoreDelta,
				"payment amount exceeds high amount threshold",
			),
		},
		"local-v1",
	)

	response := ToProtoScorePaymentResponse(result)

	if response.GetScore() != 35 {
		t.Fatalf("expected score 35, got %d", response.GetScore())
	}

	if response.GetDecision() != riskv1.RiskDecision_RISK_DECISION_APPROVED {
		t.Fatalf("expected decision %s, got %s", riskv1.RiskDecision_RISK_DECISION_APPROVED, response.GetDecision())
	}

	if len(response.GetReasonCodes()) != 1 {
		t.Fatalf("expected 1 reason code, got %d", len(response.GetReasonCodes()))
	}

	if response.GetReasonCodes()[0] != riskv1.RiskReasonCode_RISK_REASON_CODE_HIGH_AMOUNT {
		t.Fatalf("expected reason code %s, got %s", riskv1.RiskReasonCode_RISK_REASON_CODE_HIGH_AMOUNT, response.GetReasonCodes()[0])
	}

	if len(response.GetRuleHits()) != 1 {
		t.Fatalf("expected 1 rule hit, got %d", len(response.GetRuleHits()))
	}

	if response.GetRuleVersion() != "local-v1" {
		t.Fatalf("expected rule version local-v1, got %s", response.GetRuleVersion())
	}
}

func TestToProtoScorePaymentResponseMapsAllDecisionsAndReasonCodes(t *testing.T) {
	t.Parallel()

	result := risk.NewScoringResult(
		110,
		risk.DecisionDeclined,
		[]risk.ReasonCode{
			risk.ReasonCodeHighAmount,
			risk.ReasonCodeSuspiciousCurrency,
			risk.ReasonCodeRepeatedDevice,
			risk.ReasonCodeMerchantRiskThresholdExceeded,
		},
		[]risk.RuleHit{
			risk.NewRuleHit(
				risk.HighAmountRuleID,
				risk.ReasonCodeHighAmount,
				risk.HighAmountScoreDelta,
				"payment amount exceeds high amount threshold",
			),
			risk.NewRuleHit(
				risk.SuspiciousCurrencyRuleID,
				risk.ReasonCodeSuspiciousCurrency,
				risk.SuspiciousCurrencyScoreDelta,
				"suspicious currency detected",
			),
			risk.NewRuleHit(
				risk.RepeatedDeviceRuleID,
				risk.ReasonCodeRepeatedDevice,
				risk.RepeatedDeviceScoreDelta,
				"device fingerprint matched repeated-device placeholder heuristic",
			),
			risk.NewRuleHit(
				risk.MerchantRiskThresholdRuleID,
				risk.ReasonCodeMerchantRiskThresholdExceeded,
				risk.MerchantRiskThresholdScoreDelta,
				"merchant matched high-risk threshold placeholder heuristic",
			),
		},
		"local-v1",
	)

	response := ToProtoScorePaymentResponse(result)

	if response.GetDecision() != riskv1.RiskDecision_RISK_DECISION_DECLINED {
		t.Fatalf("expected declined decision, got %s", response.GetDecision())
	}

	expectedReasonCodes := []riskv1.RiskReasonCode{
		riskv1.RiskReasonCode_RISK_REASON_CODE_HIGH_AMOUNT,
		riskv1.RiskReasonCode_RISK_REASON_CODE_SUSPICIOUS_CURRENCY,
		riskv1.RiskReasonCode_RISK_REASON_CODE_REPEATED_DEVICE,
		riskv1.RiskReasonCode_RISK_REASON_CODE_MERCHANT_RISK_THRESHOLD_EXCEEDED,
	}

	if len(response.GetReasonCodes()) != len(expectedReasonCodes) {
		t.Fatalf("expected %d reason codes, got %d", len(expectedReasonCodes), len(response.GetReasonCodes()))
	}

	for i, expectedReasonCode := range expectedReasonCodes {
		if response.GetReasonCodes()[i] != expectedReasonCode {
			t.Fatalf("expected reason code %d to be %s, got %s", i, expectedReasonCode, response.GetReasonCodes()[i])
		}
	}

	if len(response.GetRuleHits()) != 4 {
		t.Fatalf("expected 4 rule hits, got %d", len(response.GetRuleHits()))
	}
}
