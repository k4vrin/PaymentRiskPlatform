package risk

import "strings"

const (
	MerchantRiskThresholdRuleID = "MERCHANT_RISK_THRESHOLD_RULE"

	// Phase 4 deterministic placeholder only.
	// Real merchant risk should come from merchant risk profile data, not merchant ID shape.
	MerchantRiskPlaceholderPrefix = "high_risk_"

	MerchantRiskThresholdScoreDelta = 25
)

type MerchantRiskThresholdRule struct{}

func NewMerchantRiskThresholdRule() MerchantRiskThresholdRule {
	return MerchantRiskThresholdRule{}
}

func (r MerchantRiskThresholdRule) Evaluate(request ScoringRequest) []RuleHit {
	merchantID := strings.ToLower(strings.TrimSpace(request.MerchantID))

	if !strings.HasPrefix(merchantID, MerchantRiskPlaceholderPrefix) {
		return []RuleHit{}
	}

	return []RuleHit{
		NewRuleHit(
			MerchantRiskThresholdRuleID,
			ReasonCodeMerchantRiskThresholdExceeded,
			MerchantRiskThresholdScoreDelta,
			"merchant matched high-risk threshold placeholder heuristic",
		),
	}
}
