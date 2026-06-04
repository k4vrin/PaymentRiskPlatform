package grpc

import (
	"context"
	"io"
	"log/slog"
	"strings"

	riskv1 "github.com/k4vrin/PaymentRiskPlatform/proto/gen/go/risk/v1"

	"github.com/k4vrin/PaymentRiskPlatform/services/risk-scoring-service/internal/risk"

	googlegrpc "google.golang.org/grpc"
)

type scorer interface {
	Score(request risk.ScoringRequest) risk.ScoringResult
}

type RiskScoringServer struct {
	riskv1.UnimplementedRiskScoringServiceServer

	scorer scorer
	logger *slog.Logger
}

func NewRiskScoringServer(scorer scorer, logger *slog.Logger) *RiskScoringServer {
	if logger == nil {
		logger = slog.New(slog.NewTextHandler(io.Discard, nil))
	}

	return &RiskScoringServer{
		scorer: scorer,
		logger: logger,
	}
}

func RegisterRiskScoringServer(grpcServer *googlegrpc.Server, server *RiskScoringServer) {
	// This binds the Go implementation to the protobuf service generated from
	// proto/risk/v1/risk_scoring.proto. The Java service calls this service over
	// a ManagedChannel using the same generated contract.
	riskv1.RegisterRiskScoringServiceServer(grpcServer, server)
}

func (s *RiskScoringServer) ScorePayment(
	ctx context.Context,
	request *riskv1.ScorePaymentRequest,
) (*riskv1.ScorePaymentResponse, error) {

	// ScorePayment is the Go side of the Java -> Go risk-scoring call. It
	// receives the protobuf request, validates it, maps it to internal risk
	// models, executes rules, and returns a protobuf response to Java.
	correlationID := correlationIDFromRequest(request)

	logger := s.logger.With(
		"payment_id", request.GetPaymentId(),
		"merchant_id", request.GetMerchantId(),
		"correlation_id", correlationID,
	)
	logger.InfoContext(ctx, "risk scoring request received")

	if err := ValidateScorePaymentRequest(request); err != nil {
		logger.WarnContext(ctx, "risk scoring request rejected", slog.String("error", err.Error()))
		return nil, err
	}

	scoringRequest := ToScoringRequest(request)
	result := s.scorer.Score(scoringRequest)

	s.logger.InfoContext(
		ctx,
		"risk score completed",
		"payment_id", scoringRequest.PaymentID,
		"merchant_id", scoringRequest.MerchantID,
		"correlation_id", scoringRequest.CorrelationID,
		"score", result.Score,
		"decision", result.Decision,
		"rule_version", result.RuleVersion,
	)

	return ToProtoScorePaymentResponse(result), nil
}

func correlationIDFromRequest(req *riskv1.ScorePaymentRequest) string {
	if req == nil {
		return ""
	}

	return strings.TrimSpace(req.GetCorrelationId())
}
