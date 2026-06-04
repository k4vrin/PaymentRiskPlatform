package grpc

import (
	"strings"

	riskv1 "github.com/k4vrin/PaymentRiskPlatform/proto/gen/go/risk/v1"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

func ValidateScorePaymentRequest(request *riskv1.ScorePaymentRequest) error {
	if request == nil {
		return status.Error(codes.InvalidArgument, "request is required")
	}

	if strings.TrimSpace(request.GetPaymentId()) == "" {
		return status.Error(codes.InvalidArgument, "payment_id is required")
	}

	if request.GetAmountMinor() <= 0 {
		return status.Error(codes.InvalidArgument, "amount_minor must be positive")
	}

	if !isValidCurrency(request.GetCurrency()) {
		return status.Error(codes.InvalidArgument, "currency must be a 3-letter uppercase ISO-style code")
	}

	if strings.TrimSpace(request.GetMerchantId()) == "" {
		return status.Error(codes.InvalidArgument, "merchant_id is required")
	}

	if strings.TrimSpace(request.GetCustomerId()) == "" {
		return status.Error(codes.InvalidArgument, "customer_id is required")
	}

	return nil
}

func isValidCurrency(currency string) bool {
	currency = strings.TrimSpace(currency)

	if len(currency) != 3 {
		return false
	}

	for _, char := range currency {
		if char < 'A' || char > 'Z' {
			return false
		}
	}

	return true
}
