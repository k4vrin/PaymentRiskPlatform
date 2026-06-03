package risk

const (
	HighAmountRuleID = "HIGH_AMOUNT_RULE"

	// Phase 4 local deterministic threshold.
	// amountMinor is in minor units, so for USD/EUR this means 100,000.00.
	HighAmountThresholdMinor = int64(10_000_000)

	HighAmountScoreDelta = 35
)

type HighAmountRule struct{}

func NewHighAmountRule() HighAmountRule {
	return HighAmountRule{}
}

func (r HighAmountRule) Evaluate(request ScoringRequest) []RuleHit {
	if request.AmountMinor <= HighAmountThresholdMinor {
		return []RuleHit{}
	}

	return []RuleHit{
		NewRuleHit(
			HighAmountRuleID,
			ReasonCodeHighAmount,
			HighAmountScoreDelta,
			"payment amount exceeds high amount threshold",
		),
	}
}
