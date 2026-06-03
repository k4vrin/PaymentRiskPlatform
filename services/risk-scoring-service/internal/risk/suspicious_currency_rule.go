package risk

import "strings"

const (
	SuspiciousCurrencyRuleID = "SUSPICIOUS_CURRENCY_RULE"

	SuspiciousCurrencyScoreDelta = 30
)

var suspiciousCurrencies = map[string]struct{}{
	"BTC": {},
	"ETH": {},
	"XTS": {},
	"XXX": {},
}

type SuspiciousCurrencyRule struct{}

func NewSuspiciousCurrencyRule() SuspiciousCurrencyRule {
	return SuspiciousCurrencyRule{}
}

func (r SuspiciousCurrencyRule) Evaluate(request ScoringRequest) []RuleHit {
	currency := strings.ToUpper(strings.TrimSpace(request.Currency))

	if _, isSuspicious := suspiciousCurrencies[currency]; !isSuspicious {
		return []RuleHit{}
	}

	return []RuleHit{
		NewRuleHit(
			SuspiciousCurrencyRuleID,
			ReasonCodeSuspiciousCurrency,
			SuspiciousCurrencyScoreDelta,
			"suspicious currency detected",
		),
	}
}
