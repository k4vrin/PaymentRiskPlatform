package risk

import "testing"

func TestHighAmountRuleBelowThreshold(t *testing.T) {
	t.Parallel()

	rule := NewHighAmountRule()

	hits := rule.Evaluate(ScoringRequest{
		AmountMinor: HighAmountThresholdMinor - 1,
	})

	if len(hits) != 0 {
		t.Fatalf("expected no rule hits, got %d", len(hits))
	}
}

func TestHighAmountRuleAtThreshold(t *testing.T) {
	t.Parallel()

	rule := NewHighAmountRule()

	hits := rule.Evaluate(ScoringRequest{
		AmountMinor: HighAmountThresholdMinor,
	})

	if len(hits) != 0 {
		t.Fatalf("expected no rule hits, got %d", len(hits))
	}
}

func TestHighAmountRuleAboveThreshold(t *testing.T) {
	t.Parallel()

	rule := NewHighAmountRule()

	hits := rule.Evaluate(ScoringRequest{
		AmountMinor: HighAmountThresholdMinor + 1,
	})

	if len(hits) != 1 {
		t.Fatalf("expected 1 rule hit, got %d", len(hits))
	}

	hit := hits[0]

	if hit.RuleID != HighAmountRuleID {
		t.Fatalf("expected rule id %s, got %s", HighAmountRuleID, hit.RuleID)
	}

	if hit.ReasonCode != ReasonCodeHighAmount {
		t.Fatalf("expected reason code %s, got %s", ReasonCodeHighAmount, hit.ReasonCode)
	}

	if hit.ScoreDelta != HighAmountScoreDelta {
		t.Fatalf("expected score delta %d, got %d", HighAmountScoreDelta, hit.ScoreDelta)
	}

	if hit.Message == "" {
		t.Fatal("expected rule hit message to be present")
	}
}
