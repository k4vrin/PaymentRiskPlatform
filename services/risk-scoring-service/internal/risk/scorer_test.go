package risk

import "testing"

func TestScorerReturnsLowRiskFallbackForCleanRequest(t *testing.T) {
	t.Parallel()

	policy, err := NewDecisionPolicy(49, 79)
	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}

	scorer := NewDefaultScorer(policy, "local-v1")

	result := scorer.Score(ScoringRequest{
		PaymentID:         "pay_123",
		MerchantID:        "merchant_123",
		CustomerID:        "customer_123",
		AmountMinor:       1000,
		Currency:          "USD",
		DeviceFingerprint: "device_123",
		CorrelationID:     "corr_123",
	})

	if result.Score != 0 {
		t.Fatalf("expected score 0, got %d", result.Score)
	}

	if result.Decision != DecisionApproved {
		t.Fatalf("expected decision %s, got %s", DecisionApproved, result.Decision)
	}

	if len(result.RuleHits) != 1 {
		t.Fatalf("expected 1 fallback rule hit, got %d", len(result.RuleHits))
	}

	if result.RuleHits[0].RuleID != LowRiskRuleID {
		t.Fatalf("expected fallback rule id %s, got %s", LowRiskRuleID, result.RuleHits[0].RuleID)
	}

	if len(result.ReasonCodes) != 1 {
		t.Fatalf("expected 1 reason code, got %d", len(result.ReasonCodes))
	}

	if result.ReasonCodes[0] != ReasonCodeLowRiskPayment {
		t.Fatalf("expected reason code %s, got %s", ReasonCodeLowRiskPayment, result.ReasonCodes[0])
	}

	if result.RuleVersion != "local-v1" {
		t.Fatalf("expected rule version local-v1, got %s", result.RuleVersion)
	}
}

func TestScorerAggregatesMultipleRulesInDeterministicOrder(t *testing.T) {
	t.Parallel()

	policy, err := NewDecisionPolicy(49, 79)
	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}

	scorer := NewDefaultScorer(policy, "local-v1")

	result := scorer.Score(ScoringRequest{
		PaymentID:         "pay_123",
		MerchantID:        "high_risk_merchant_123",
		CustomerID:        "customer_123",
		AmountMinor:       HighAmountThresholdMinor + 1,
		Currency:          "XXX",
		DeviceFingerprint: "repeat_device_123",
		CorrelationID:     "corr_123",
	})

	expectedScore := HighAmountScoreDelta +
		SuspiciousCurrencyScoreDelta +
		RepeatedDeviceScoreDelta +
		MerchantRiskThresholdScoreDelta

	if result.Score != expectedScore {
		t.Fatalf("expected score %d, got %d", expectedScore, result.Score)
	}

	if result.Decision != DecisionDeclined {
		t.Fatalf("expected decision %s, got %s", DecisionDeclined, result.Decision)
	}

	expectedRuleIDs := []string{
		HighAmountRuleID,
		SuspiciousCurrencyRuleID,
		RepeatedDeviceRuleID,
		MerchantRiskThresholdRuleID,
	}

	if len(result.RuleHits) != len(expectedRuleIDs) {
		t.Fatalf("expected %d rule hits, got %d", len(expectedRuleIDs), len(result.RuleHits))
	}

	for i, expectedRuleID := range expectedRuleIDs {
		if result.RuleHits[i].RuleID != expectedRuleID {
			t.Fatalf("expected rule hit %d to be %s, got %s", i, expectedRuleID, result.RuleHits[i].RuleID)
		}
	}

	expectedReasonCodes := []ReasonCode{
		ReasonCodeHighAmount,
		ReasonCodeSuspiciousCurrency,
		ReasonCodeRepeatedDevice,
		ReasonCodeMerchantRiskThresholdExceeded,
	}

	if len(result.ReasonCodes) != len(expectedReasonCodes) {
		t.Fatalf("expected %d reason codes, got %d", len(expectedReasonCodes), len(result.ReasonCodes))
	}

	for i, expectedReasonCode := range expectedReasonCodes {
		if result.ReasonCodes[i] != expectedReasonCode {
			t.Fatalf("expected reason code %d to be %s, got %s", i, expectedReasonCode, result.ReasonCodes[i])
		}
	}

	if result.RuleVersion != "local-v1" {
		t.Fatalf("expected rule version local-v1, got %s", result.RuleVersion)
	}
}

func TestScorerDeduplicatesReasonCodesPreservingFirstSeenOrder(t *testing.T) {
	t.Parallel()

	policy, err := NewDecisionPolicy(49, 79)
	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}

	scorer := NewScorer(
		[]Rule{
			fakeRule{
				hits: []RuleHit{
					NewRuleHit("RULE_1", ReasonCodeHighAmount, 10, "first high amount hit"),
					NewRuleHit("RULE_2", ReasonCodeHighAmount, 15, "second high amount hit"),
					NewRuleHit("RULE_3", ReasonCodeRepeatedDevice, 20, "repeated device hit"),
				},
			},
		},
		policy,
		"local-v1",
	)

	result := scorer.Score(ScoringRequest{})

	expectedReasonCodes := []ReasonCode{
		ReasonCodeHighAmount,
		ReasonCodeRepeatedDevice,
	}

	if len(result.ReasonCodes) != len(expectedReasonCodes) {
		t.Fatalf("expected %d reason codes, got %d", len(expectedReasonCodes), len(result.ReasonCodes))
	}

	for i, expectedReasonCode := range expectedReasonCodes {
		if result.ReasonCodes[i] != expectedReasonCode {
			t.Fatalf("expected reason code %d to be %s, got %s", i, expectedReasonCode, result.ReasonCodes[i])
		}
	}
}

type fakeRule struct {
	hits []RuleHit
}

func (r fakeRule) Evaluate(_ ScoringRequest) []RuleHit {
	return r.hits
}
