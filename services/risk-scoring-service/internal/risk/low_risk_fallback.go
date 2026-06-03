package risk

const (
	LowRiskRuleID = "LOW_RISK_RULE"

	LowRiskScoreDelta = 0
)

func ApplyLowRiskFallback(ruleHits []RuleHit) []RuleHit {
	if len(ruleHits) > 0 {
		return append([]RuleHit{}, ruleHits...)
	}

	return []RuleHit{
		NewRuleHit(
			LowRiskRuleID,
			ReasonCodeLowRiskPayment,
			LowRiskScoreDelta,
			"no positive-risk rules matched",
		),
	}
}
