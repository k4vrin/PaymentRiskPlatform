package risk

import "testing"

func TestApplyLowRiskFallbackAddsLowRiskHitWhenNoRulesMatched(t *testing.T) {
	t.Parallel()

	hits := ApplyLowRiskFallback([]RuleHit{})

	if len(hits) != 1 {
		t.Fatalf("expected 1 fallback rule hit, got %d", len(hits))
	}

	hit := hits[0]

	if hit.RuleID != LowRiskRuleID {
		t.Fatalf("expected rule id %s, got %s", LowRiskRuleID, hit.RuleID)
	}

	if hit.ReasonCode != ReasonCodeLowRiskPayment {
		t.Fatalf("expected reason code %s, got %s", ReasonCodeLowRiskPayment, hit.ReasonCode)
	}

	if hit.ScoreDelta != LowRiskScoreDelta {
		t.Fatalf("expected score delta %d, got %d", LowRiskScoreDelta, hit.ScoreDelta)
	}

	if hit.Message == "" {
		t.Fatal("expected fallback message to be present")
	}
}

func TestApplyLowRiskFallbackAddsLowRiskHitForNilInput(t *testing.T) {
	t.Parallel()

	hits := ApplyLowRiskFallback(nil)

	if len(hits) != 1 {
		t.Fatalf("expected 1 fallback rule hit, got %d", len(hits))
	}

	if hits[0].ReasonCode != ReasonCodeLowRiskPayment {
		t.Fatalf("expected reason code %s, got %s", ReasonCodeLowRiskPayment, hits[0].ReasonCode)
	}
}

func TestApplyLowRiskFallbackDoesNotHidePositiveRuleHits(t *testing.T) {
	t.Parallel()

	positiveHit := NewRuleHit(
		HighAmountRuleID,
		ReasonCodeHighAmount,
		HighAmountScoreDelta,
		"payment amount exceeds high amount threshold",
	)

	hits := ApplyLowRiskFallback([]RuleHit{positiveHit})

	if len(hits) != 1 {
		t.Fatalf("expected original positive hit only, got %d hits", len(hits))
	}

	if hits[0].RuleID != HighAmountRuleID {
		t.Fatalf("expected original rule id %s, got %s", HighAmountRuleID, hits[0].RuleID)
	}

	if hits[0].ReasonCode == ReasonCodeLowRiskPayment {
		t.Fatal("low-risk fallback should not be added when positive-risk rules exist")
	}
}

func TestApplyLowRiskFallbackCopiesPositiveRuleHits(t *testing.T) {
	t.Parallel()

	positiveHit := NewRuleHit(
		HighAmountRuleID,
		ReasonCodeHighAmount,
		HighAmountScoreDelta,
		"payment amount exceeds high amount threshold",
	)
	sourceHits := []RuleHit{positiveHit}

	hits := ApplyLowRiskFallback(sourceHits)
	sourceHits[0] = NewRuleHit(
		SuspiciousCurrencyRuleID,
		ReasonCodeSuspiciousCurrency,
		SuspiciousCurrencyScoreDelta,
		"suspicious currency detected",
	)

	if hits[0].RuleID != HighAmountRuleID {
		t.Fatalf("expected copied rule id %s, got %s", HighAmountRuleID, hits[0].RuleID)
	}
}
