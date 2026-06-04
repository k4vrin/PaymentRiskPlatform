package grpc

import (
	"testing"

	riskv1 "github.com/k4vrin/PaymentRiskPlatform/proto/gen/go/risk/v1"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

func TestValidateScorePaymentRequestValid(t *testing.T) {
	t.Parallel()

	err := ValidateScorePaymentRequest(validScorePaymentRequest())

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
}

func TestValidateScorePaymentRequestInvalid(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name    string
		request *riskv1.ScorePaymentRequest
	}{
		{
			name:    "nil request",
			request: nil,
		},
		{
			name: "missing payment id",
			request: func() *riskv1.ScorePaymentRequest {
				request := validScorePaymentRequest()
				request.PaymentId = " "
				return request
			}(),
		},
		{
			name: "non-positive amount",
			request: func() *riskv1.ScorePaymentRequest {
				request := validScorePaymentRequest()
				request.AmountMinor = 0
				return request
			}(),
		},
		{
			name: "missing currency",
			request: func() *riskv1.ScorePaymentRequest {
				request := validScorePaymentRequest()
				request.Currency = ""
				return request
			}(),
		},
		{
			name: "lowercase currency",
			request: func() *riskv1.ScorePaymentRequest {
				request := validScorePaymentRequest()
				request.Currency = "usd"
				return request
			}(),
		},
		{
			name: "short currency",
			request: func() *riskv1.ScorePaymentRequest {
				request := validScorePaymentRequest()
				request.Currency = "US"
				return request
			}(),
		},
		{
			name: "long currency",
			request: func() *riskv1.ScorePaymentRequest {
				request := validScorePaymentRequest()
				request.Currency = "USDD"
				return request
			}(),
		},
		{
			name: "malformed currency",
			request: func() *riskv1.ScorePaymentRequest {
				request := validScorePaymentRequest()
				request.Currency = "US1"
				return request
			}(),
		},
		{
			name: "missing merchant id",
			request: func() *riskv1.ScorePaymentRequest {
				request := validScorePaymentRequest()
				request.MerchantId = " "
				return request
			}(),
		},
		{
			name: "missing customer id",
			request: func() *riskv1.ScorePaymentRequest {
				request := validScorePaymentRequest()
				request.CustomerId = " "
				return request
			}(),
		},
	}

	for _, tt := range tests {
		tt := tt

		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			err := ValidateScorePaymentRequest(tt.request)

			if err == nil {
				t.Fatal("expected validation error, got nil")
			}

			if status.Code(err) != codes.InvalidArgument {
				t.Fatalf("expected gRPC code %s, got %s", codes.InvalidArgument, status.Code(err))
			}
		})
	}
}

func validScorePaymentRequest() *riskv1.ScorePaymentRequest {
	return &riskv1.ScorePaymentRequest{
		PaymentId:         "pay_123",
		MerchantId:        "merchant_123",
		CustomerId:        "customer_123",
		AmountMinor:       1000,
		Currency:          "USD",
		DeviceFingerprint: "device_123",
		CorrelationId:     "corr_123",
	}
}
