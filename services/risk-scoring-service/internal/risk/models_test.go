package risk

import "testing"

func TestNewRuleHit(t *testing.T) {
	t.Parallel()

	hit := NewRuleHit(
		"HIGH_AMOUNT_RULE",
		ReasonCodeHighAmount,
		25,
		"amount exceeds configured threshold",
	)

	if hit.RuleID != "HIGH_AMOUNT_RULE" {
		t.Fatalf("expected rule id HIGH_AMOUNT_RULE, got %s", hit.RuleID)
	}

	if hit.ReasonCode != ReasonCodeHighAmount {
		t.Fatalf("expected reason code %s, got %s", ReasonCodeHighAmount, hit.ReasonCode)
	}

	if hit.ScoreDelta != 25 {
		t.Fatalf("expected score delta 25, got %d", hit.ScoreDelta)
	}

	if hit.Message == "" {
		t.Fatal("expected message to be present")
	}
}

func TestNewScoringResult(t *testing.T) {
	t.Parallel()

	hit := NewRuleHit(
		"LOW_RISK_RULE",
		ReasonCodeLowRiskPayment,
		0,
		"no positive risk rules matched",
	)

	result := NewScoringResult(
		10,
		DecisionApproved,
		[]ReasonCode{ReasonCodeLowRiskPayment},
		[]RuleHit{hit},
		"local-v1",
	)

	if result.Score != 10 {
		t.Fatalf("expected score 10, got %d", result.Score)
	}

	if result.Decision != DecisionApproved {
		t.Fatalf("expected decision %s, got %s", DecisionApproved, result.Decision)
	}

	if len(result.ReasonCodes) != 1 {
		t.Fatalf("expected 1 reason code, got %d", len(result.ReasonCodes))
	}

	if len(result.RuleHits) != 1 {
		t.Fatalf("expected 1 rule hit, got %d", len(result.RuleHits))
	}

	if result.RuleVersion != "local-v1" {
		t.Fatalf("expected rule version local-v1, got %s", result.RuleVersion)
	}
}

func TestNewScoringResultCopiesSlices(t *testing.T) {
	t.Parallel()

	reasonCodes := []ReasonCode{ReasonCodeLowRiskPayment}
	ruleHits := []RuleHit{
		NewRuleHit(
			"LOW_RISK_RULE",
			ReasonCodeLowRiskPayment,
			0,
			"no positive risk rules matched",
		),
	}

	result := NewScoringResult(
		0,
		DecisionApproved,
		reasonCodes,
		ruleHits,
		"local-v1",
	)

	reasonCodes[0] = ReasonCodeHighAmount
	ruleHits[0] = NewRuleHit(
		"HIGH_AMOUNT_RULE",
		ReasonCodeHighAmount,
		35,
		"amount exceeds configured threshold",
	)

	if result.ReasonCodes[0] != ReasonCodeLowRiskPayment {
		t.Fatalf("expected copied reason code to remain %s, got %s", ReasonCodeLowRiskPayment, result.ReasonCodes[0])
	}

	if result.RuleHits[0].RuleID != "LOW_RISK_RULE" {
		t.Fatalf("expected copied rule hit to remain LOW_RISK_RULE, got %s", result.RuleHits[0].RuleID)
	}
}

func TestNewScoringResultInitializesEmptySlices(t *testing.T) {
	t.Parallel()

	result := NewScoringResult(
		0,
		DecisionApproved,
		nil,
		nil,
		"local-v1",
	)

	if result.ReasonCodes == nil {
		t.Fatal("expected reason codes to be an initialized empty slice")
	}

	if result.RuleHits == nil {
		t.Fatal("expected rule hits to be an initialized empty slice")
	}
}

func TestScoringRequestHoldsProtoContractFieldsWithoutProtoDependency(t *testing.T) {
	t.Parallel()

	request := ScoringRequest{
		PaymentID:         "pay_123",
		MerchantID:        "mer_123",
		CustomerID:        "cus_123",
		AmountMinor:       1299,
		Currency:          "USD",
		DeviceFingerprint: "device_123",
		CorrelationID:     "corr_123",
	}

	if request.PaymentID != "pay_123" {
		t.Fatalf("expected payment id pay_123, got %s", request.PaymentID)
	}

	if request.AmountMinor != 1299 {
		t.Fatalf("expected amount 1299, got %d", request.AmountMinor)
	}

	if request.Currency != "USD" {
		t.Fatalf("expected currency USD, got %s", request.Currency)
	}

	if request.MerchantID != "mer_123" {
		t.Fatalf("expected merchant id mer_123, got %s", request.MerchantID)
	}

	if request.CustomerID != "cus_123" {
		t.Fatalf("expected customer id cus_123, got %s", request.CustomerID)
	}

	if request.DeviceFingerprint != "device_123" {
		t.Fatalf("expected device fingerprint device_123, got %s", request.DeviceFingerprint)
	}

	if request.CorrelationID != "corr_123" {
		t.Fatalf("expected correlation id corr_123, got %s", request.CorrelationID)
	}
}
