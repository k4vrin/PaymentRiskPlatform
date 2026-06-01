package dev.kavrin.paymentrisk.risk.infrastructure.grpc;

import dev.kavrin.paymentrisk.risk.application.RiskScoringClient;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringOutcome;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringRequest;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringResponse;
import dev.kavrin.paymentrisk.risk.application.dto.RiskRuleHitSummary;
import dev.kavrin.paymentrisk.risk.v1.RiskDecision;
import dev.kavrin.paymentrisk.risk.v1.RiskReasonCode;
import dev.kavrin.paymentrisk.risk.v1.RiskRuleHit;
import dev.kavrin.paymentrisk.risk.v1.RiskScoringServiceGrpc;
import dev.kavrin.paymentrisk.risk.v1.ScorePaymentRequest;
import dev.kavrin.paymentrisk.risk.v1.ScorePaymentResponse;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Component
public class GrpcRiskScoringClient implements RiskScoringClient {

    private final RiskGrpcProperties properties;
    private final RiskScoringServiceGrpc.RiskScoringServiceStub stub;

    @Autowired
    public GrpcRiskScoringClient(
            RiskGrpcProperties properties,
            ManagedChannel channel
    ) {
        this(properties, RiskScoringServiceGrpc.newStub(channel));
    }

    GrpcRiskScoringClient(
            RiskGrpcProperties properties,
            RiskScoringServiceGrpc.RiskScoringServiceStub stub
    ) {
        this.properties = properties;
        this.stub = stub;
    }

    @Override
    public Mono<RiskScoringResponse> score(RiskScoringRequest request) {
        ScorePaymentRequest grpcRequest = toGrpcRequest(request);

        return Mono.create(sink ->
                stub.withDeadlineAfter(properties.timeout().toMillis(), TimeUnit.MILLISECONDS)
                        .scorePayment(grpcRequest, new StreamObserver<>() {
                            @Override
                            public void onNext(ScorePaymentResponse response) {
                                sink.success(toInternalResponse(response));
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                sink.success(toFailureResponse(throwable));
                            }

                            @Override
                            public void onCompleted() {
                                // Response is completed in onNext.
                            }
                        })
        );
    }

    private static ScorePaymentRequest toGrpcRequest(RiskScoringRequest request) {
        return ScorePaymentRequest.newBuilder()
                .setPaymentId(request.paymentId())
                .setAmountMinor(request.amountMinor())
                .setCurrency(request.currency())
                .setMerchantId(request.merchantId())
                .setCustomerId(request.customerId())
                .setDeviceFingerprint(request.deviceFingerprint())
                .setCorrelationId(request.correlationId())
                .build();
    }

    private static RiskScoringResponse toInternalResponse(ScorePaymentResponse response) {
        return new RiskScoringResponse(
                toInternalOutcome(response.getDecision()),
                response.getScore(),
                response.getReasonCodesList().stream()
                        .map(GrpcRiskScoringClient::toInternalReasonCode)
                        .toList(),
                response.getRuleHitsList().stream()
                        .map(GrpcRiskScoringClient::toInternalRuleHit)
                        .toList(),
                response.getRuleVersion()
        );
    }

    private static RiskRuleHitSummary toInternalRuleHit(RiskRuleHit ruleHit) {
        return new RiskRuleHitSummary(
                ruleHit.getRuleId(),
                toInternalReasonCode(ruleHit.getReasonCode()),
                ruleHit.getScoreDelta(),
                ruleHit.getMessage()
        );
    }

    private static RiskScoringOutcome toInternalOutcome(RiskDecision decision) {
        return switch (decision) {
            case RISK_DECISION_APPROVED -> RiskScoringOutcome.APPROVED;
            case RISK_DECISION_DECLINED -> RiskScoringOutcome.DECLINED;
            case RISK_DECISION_REVIEW_REQUIRED -> RiskScoringOutcome.REVIEW_REQUIRED;
            case RISK_DECISION_UNSPECIFIED, UNRECOGNIZED -> RiskScoringOutcome.UNAVAILABLE;
        };
    }

    private static String toInternalReasonCode(RiskReasonCode reasonCode) {
        String name = reasonCode.name();

        if (name.startsWith("RISK_REASON_CODE_")) {
            return name.substring("RISK_REASON_CODE_".length());
        }

        return name;
    }

    private static RiskScoringResponse toFailureResponse(Throwable throwable) {
        Status status = switch (throwable) {
            case StatusRuntimeException grpcException -> grpcException.getStatus();
            case StatusException grpcException -> grpcException.getStatus();
            default -> null;
        };

        if (status != null) {
            Status.Code code = status.getCode();

            if (code == Status.Code.DEADLINE_EXCEEDED) {
                return RiskScoringResponse.timeout();
            }

            if (code == Status.Code.UNAVAILABLE) {
                return RiskScoringResponse.unavailable();
            }
        }

        return RiskScoringResponse.unavailable();
    }
}
