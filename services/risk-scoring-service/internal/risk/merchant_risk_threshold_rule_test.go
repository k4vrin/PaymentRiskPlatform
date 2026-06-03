package risk

import "testing"

func TestMerchantRiskThresholdRuleLowRiskMerchant(t *testing.T) {
	t.Parallel()

	rule := NewMerchantRiskThresholdRule()

	hits := rule.Evaluate(ScoringRequest{
		MerchantID: "merchant_123",
	})

	if len(hits) != 0 {
		t.Fatalf("expected no rule hits, got %d", len(hits))
	}
}

func TestMerchantRiskThresholdRuleHighRiskMerchant(t *testing.T) {
	t.Parallel()

	rule := NewMerchantRiskThresholdRule()

	hits := rule.Evaluate(ScoringRequest{
		MerchantID: "high_risk_merchant_123",
	})

	if len(hits) != 1 {
		t.Fatalf("expected 1 rule hit, got %d", len(hits))
	}

	hit := hits[0]

	if hit.RuleID != MerchantRiskThresholdRuleID {
		t.Fatalf("expected rule id %s, got %s", MerchantRiskThresholdRuleID, hit.RuleID)
	}

	if hit.ReasonCode != ReasonCodeMerchantRiskThresholdExceeded {
		t.Fatalf("expected reason code %s, got %s", ReasonCodeMerchantRiskThresholdExceeded, hit.ReasonCode)
	}

	if hit.ScoreDelta != MerchantRiskThresholdScoreDelta {
		t.Fatalf("expected score delta %d, got %d", MerchantRiskThresholdScoreDelta, hit.ScoreDelta)
	}

	if hit.Message == "" {
		t.Fatal("expected rule hit message to be present")
	}
}

func TestMerchantRiskThresholdRuleIsCaseInsensitiveAndTrimsWhitespace(t *testing.T) {
	t.Parallel()

	rule := NewMerchantRiskThresholdRule()

	hits := rule.Evaluate(ScoringRequest{
		MerchantID: "  HIGH_RISK_merchant_123  ",
	})

	if len(hits) != 1 {
		t.Fatalf("expected 1 rule hit, got %d", len(hits))
	}
}
