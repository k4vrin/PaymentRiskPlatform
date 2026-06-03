package risk

import "testing"

func TestSuspiciousCurrencyRuleNormalCurrency(t *testing.T) {
	t.Parallel()

	rule := NewSuspiciousCurrencyRule()

	hits := rule.Evaluate(ScoringRequest{
		Currency: "USD",
	})

	if len(hits) != 0 {
		t.Fatalf("expected no rule hits, got %d", len(hits))
	}
}

func TestSuspiciousCurrencyRuleSuspiciousCurrency(t *testing.T) {
	t.Parallel()

	rule := NewSuspiciousCurrencyRule()

	hits := rule.Evaluate(ScoringRequest{
		Currency: "XXX",
	})

	if len(hits) != 1 {
		t.Fatalf("expected 1 rule hit, got %d", len(hits))
	}

	hit := hits[0]

	if hit.RuleID != SuspiciousCurrencyRuleID {
		t.Fatalf("expected rule id %s, got %s", SuspiciousCurrencyRuleID, hit.RuleID)
	}

	if hit.ReasonCode != ReasonCodeSuspiciousCurrency {
		t.Fatalf("expected reason code %s, got %s", ReasonCodeSuspiciousCurrency, hit.ReasonCode)
	}

	if hit.ScoreDelta != SuspiciousCurrencyScoreDelta {
		t.Fatalf("expected score delta %d, got %d", SuspiciousCurrencyScoreDelta, hit.ScoreDelta)
	}

	if hit.Message == "" {
		t.Fatal("expected rule hit message to be present")
	}
}

func TestSuspiciousCurrencyRuleLowercaseCurrency(t *testing.T) {
	t.Parallel()

	rule := NewSuspiciousCurrencyRule()

	hits := rule.Evaluate(ScoringRequest{
		Currency: "xxx",
	})

	if len(hits) != 1 {
		t.Fatalf("expected 1 rule hit, got %d", len(hits))
	}
}

func TestSuspiciousCurrencyRuleTrimsWhitespace(t *testing.T) {
	t.Parallel()

	rule := NewSuspiciousCurrencyRule()

	hits := rule.Evaluate(ScoringRequest{
		Currency: "  XXX  ",
	})

	if len(hits) != 1 {
		t.Fatalf("expected 1 rule hit, got %d", len(hits))
	}
}
