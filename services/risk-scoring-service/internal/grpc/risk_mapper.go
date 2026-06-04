// services/risk-scoring-service/internal/grpc/risk_mapper.go
package grpc

import (
	riskv1 "github.com/k4vrin/PaymentRiskPlatform/proto/gen/go/risk/v1"

	"github.com/k4vrin/PaymentRiskPlatform/services/risk-scoring-service/internal/risk"
)

func ToScoringRequest(request *riskv1.ScorePaymentRequest) risk.ScoringRequest {
	return risk.ScoringRequest{
		PaymentID:         request.GetPaymentId(),
		MerchantID:        request.GetMerchantId(),
		CustomerID:        request.GetCustomerId(),
		AmountMinor:       request.GetAmountMinor(),
		Currency:          request.GetCurrency(),
		DeviceFingerprint: request.GetDeviceFingerprint(),
		CorrelationID:     request.GetCorrelationId(),
	}
}

func ToProtoRiskDecision(decision risk.Decision) riskv1.RiskDecision {
	switch decision {
	case risk.DecisionApproved:
		return riskv1.RiskDecision_RISK_DECISION_APPROVED
	case risk.DecisionReviewRequired:
		return riskv1.RiskDecision_RISK_DECISION_REVIEW_REQUIRED
	case risk.DecisionDeclined:
		return riskv1.RiskDecision_RISK_DECISION_DECLINED
	default:
		return riskv1.RiskDecision_RISK_DECISION_UNSPECIFIED
	}
}

func ToProtoReasonCode(reasonCode risk.ReasonCode) riskv1.RiskReasonCode {
	switch reasonCode {
	case risk.ReasonCodeLowRiskPayment:
		return riskv1.RiskReasonCode_RISK_REASON_CODE_LOW_RISK_PAYMENT
	case risk.ReasonCodeHighAmount:
		return riskv1.RiskReasonCode_RISK_REASON_CODE_HIGH_AMOUNT
	case risk.ReasonCodeSuspiciousCurrency:
		return riskv1.RiskReasonCode_RISK_REASON_CODE_SUSPICIOUS_CURRENCY
	case risk.ReasonCodeRepeatedDevice:
		return riskv1.RiskReasonCode_RISK_REASON_CODE_REPEATED_DEVICE
	case risk.ReasonCodeMerchantRiskThresholdExceeded:
		return riskv1.RiskReasonCode_RISK_REASON_CODE_MERCHANT_RISK_THRESHOLD_EXCEEDED
	default:
		return riskv1.RiskReasonCode_RISK_REASON_CODE_UNSPECIFIED
	}
}

func ToProtoRuleHit(hit risk.RuleHit) *riskv1.RiskRuleHit {
	return &riskv1.RiskRuleHit{
		RuleId:     hit.RuleID,
		ReasonCode: ToProtoReasonCode(hit.ReasonCode),
		ScoreDelta: int32(hit.ScoreDelta),
		Message:    hit.Message,
	}
}

func ToProtoScorePaymentResponse(result risk.ScoringResult) *riskv1.ScorePaymentResponse {
	reasonCodes := make([]riskv1.RiskReasonCode, 0, len(result.ReasonCodes))
	for _, reasonCode := range result.ReasonCodes {
		reasonCodes = append(reasonCodes, ToProtoReasonCode(reasonCode))
	}

	ruleHits := make([]*riskv1.RiskRuleHit, 0, len(result.RuleHits))
	for _, hit := range result.RuleHits {
		ruleHits = append(ruleHits, ToProtoRuleHit(hit))
	}

	return &riskv1.ScorePaymentResponse{
		Score:       int32(result.Score),
		Decision:    ToProtoRiskDecision(result.Decision),
		ReasonCodes: reasonCodes,
		RuleHits:    ruleHits,
		RuleVersion: result.RuleVersion,
	}
}
