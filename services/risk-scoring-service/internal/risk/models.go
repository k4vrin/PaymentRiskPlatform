package risk

type Decision string

const (
	DecisionApproved       Decision = "APPROVED"
	DecisionReviewRequired Decision = "REVIEW_REQUIRED"
	DecisionDeclined       Decision = "DECLINED"
)

type ReasonCode string

const (
	ReasonCodeLowRiskPayment                ReasonCode = "LOW_RISK_PAYMENT"
	ReasonCodeHighAmount                    ReasonCode = "HIGH_AMOUNT"
	ReasonCodeSuspiciousCurrency            ReasonCode = "SUSPICIOUS_CURRENCY"
	ReasonCodeRepeatedDevice                ReasonCode = "REPEATED_DEVICE"
	ReasonCodeMerchantRiskThresholdExceeded ReasonCode = "MERCHANT_RISK_THRESHOLD_EXCEEDED"
)

type ScoringRequest struct {
	PaymentID         string
	MerchantID        string
	CustomerID        string
	AmountMinor       int64
	Currency          string
	DeviceFingerprint string
	CorrelationID     string
}

type RuleHit struct {
	RuleID     string
	ReasonCode ReasonCode
	ScoreDelta int
	Message    string
}

type ScoringResult struct {
	Score       int
	Decision    Decision
	ReasonCodes []ReasonCode
	RuleHits    []RuleHit
	RuleVersion string
}

func NewRuleHit(ruleID string, reasonCode ReasonCode, scoreDelta int, message string) RuleHit {
	return RuleHit{
		RuleID:     ruleID,
		ReasonCode: reasonCode,
		ScoreDelta: scoreDelta,
		Message:    message,
	}
}

func NewScoringResult(
	score int,
	decision Decision,
	reasonCodes []ReasonCode,
	ruleHits []RuleHit,
	ruleVersion string,
) ScoringResult {
	copiedReasonCodes := append([]ReasonCode{}, reasonCodes...)
	copiedRuleHits := append([]RuleHit{}, ruleHits...)

	return ScoringResult{
		Score:       score,
		Decision:    decision,
		ReasonCodes: copiedReasonCodes,
		RuleHits:    copiedRuleHits,
		RuleVersion: ruleVersion,
	}
}
