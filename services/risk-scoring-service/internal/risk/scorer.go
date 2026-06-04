package risk

type Rule interface {
	Evaluate(request ScoringRequest) []RuleHit
}

type Scorer struct {
	rules       []Rule
	policy      DecisionPolicy
	ruleVersion string
}

func NewScorer(rules []Rule, policy DecisionPolicy, ruleVersion string) Scorer {
	copiedRules := append([]Rule{}, rules...)

	return Scorer{
		rules:       copiedRules,
		policy:      policy,
		ruleVersion: ruleVersion,
	}
}

func NewDefaultScorer(policy DecisionPolicy, ruleVersion string) Scorer {
	return NewScorer(
		[]Rule{
			NewHighAmountRule(),
			NewSuspiciousCurrencyRule(),
			NewRepeatedDeviceRule(),
			NewMerchantRiskThresholdRule(),
		},
		policy,
		ruleVersion,
	)
}

func (s Scorer) Score(request ScoringRequest) ScoringResult {
	var score int
	var hits []RuleHit

	for _, rule := range s.rules {
		ruleHits := rule.Evaluate(request)
		hits = append(hits, ruleHits...)
	}

	hits = ApplyLowRiskFallback(hits)

	for _, hit := range hits {
		score += hit.ScoreDelta
	}

	reasonCodes := deduplicateReasonCodes(hits)
	decision := s.policy.Decide(score)

	return NewScoringResult(
		score,
		decision,
		reasonCodes,
		hits,
		s.ruleVersion,
	)
}

func deduplicateReasonCodes(hits []RuleHit) []ReasonCode {
	seen := map[ReasonCode]struct{}{}
	reasonCodes := make([]ReasonCode, 0, len(hits))

	for _, hit := range hits {
		if _, exists := seen[hit.ReasonCode]; exists {
			continue
		}

		seen[hit.ReasonCode] = struct{}{}
		reasonCodes = append(reasonCodes, hit.ReasonCode)
	}

	return reasonCodes
}
