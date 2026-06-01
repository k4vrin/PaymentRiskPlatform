package dev.kavrin.paymentrisk.risk.infrastructure.grpc;

import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringOutcome;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringRequest;
import dev.kavrin.paymentrisk.risk.v1.*;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class GrpcRiskScoringClientTest {

    private final RiskScoringServiceGrpc.RiskScoringServiceStub stub =
            mock(RiskScoringServiceGrpc.RiskScoringServiceStub.class);
    private final RiskGrpcProperties properties =
            new RiskGrpcProperties("risk-service", 9091, Duration.ofMillis(750));
    private final GrpcRiskScoringClient client =
            new GrpcRiskScoringClient(properties, stub);

    @BeforeEach
    void configureStub() {
        reset(stub);
        when(stub.withDeadlineAfter(750, TimeUnit.MILLISECONDS)).thenReturn(stub);
    }

    @Test
    void scoreMapsInternalRequestToGrpcRequestAndApprovedResponse() {
        ScorePaymentResponse grpcResponse = ScorePaymentResponse.newBuilder()
                .setDecision(RiskDecision.RISK_DECISION_APPROVED)
                .setScore(12)
                .addReasonCodes(RiskReasonCode.RISK_REASON_CODE_LOW_RISK_PAYMENT)
                .setRuleVersion("risk-rules-v1")
                .build();
        respondWith(grpcResponse);

        StepVerifier.create(client.score(validRequest()))
                .assertNext(response -> {
                    assertThat(response.outcome()).isEqualTo(RiskScoringOutcome.APPROVED);
                    assertThat(response.score()).isEqualTo(12);
                    assertThat(response.reasonCodes()).containsExactly("LOW_RISK_PAYMENT");
                    assertThat(response.ruleVersion()).isEqualTo("risk-rules-v1");
                })
                .verifyComplete();

        ArgumentCaptor<ScorePaymentRequest> requestCaptor =
                ArgumentCaptor.forClass(ScorePaymentRequest.class);
        verify(stub).withDeadlineAfter(750, TimeUnit.MILLISECONDS);
        verify(stub).scorePayment(requestCaptor.capture(), any(StreamObserver.class));

        ScorePaymentRequest grpcRequest = requestCaptor.getValue();
        assertThat(grpcRequest.getPaymentId()).isEqualTo("pay_test_123");
        assertThat(grpcRequest.getAmountMinor()).isEqualTo(10_000);
        assertThat(grpcRequest.getCurrency()).isEqualTo("USD");
        assertThat(grpcRequest.getMerchantId()).isEqualTo("merchant_123");
        assertThat(grpcRequest.getCustomerId()).isEqualTo("customer_123");
        assertThat(grpcRequest.getDeviceFingerprint()).isEqualTo("device_123");
        assertThat(grpcRequest.getCorrelationId()).isEqualTo("corr_123");
    }

    @Test
    void scoreMapsReviewRequiredResponse() {
        respondWith(ScorePaymentResponse.newBuilder()
                .setDecision(RiskDecision.RISK_DECISION_REVIEW_REQUIRED)
                .setScore(61)
                .addReasonCodes(RiskReasonCode.RISK_REASON_CODE_REPEATED_DEVICE)
                .setRuleVersion("risk-rules-v1")
                .build());

        StepVerifier.create(client.score(validRequest()))
                .assertNext(response -> {
                    assertThat(response.outcome()).isEqualTo(RiskScoringOutcome.REVIEW_REQUIRED);
                    assertThat(response.score()).isEqualTo(61);
                    assertThat(response.reasonCodes()).containsExactly("REPEATED_DEVICE");
                    assertThat(response.ruleVersion()).isEqualTo("risk-rules-v1");
                })
                .verifyComplete();
    }

    @Test
    void scoreMapsDeclinedResponse() {
        respondWith(ScorePaymentResponse.newBuilder()
                .setDecision(RiskDecision.RISK_DECISION_DECLINED)
                .setScore(95)
                .addReasonCodes(RiskReasonCode.RISK_REASON_CODE_HIGH_AMOUNT)
                .setRuleVersion("risk-rules-v1")
                .build());

        StepVerifier.create(client.score(validRequest()))
                .assertNext(response -> {
                    assertThat(response.outcome()).isEqualTo(RiskScoringOutcome.DECLINED);
                    assertThat(response.score()).isEqualTo(95);
                    assertThat(response.reasonCodes()).containsExactly("HIGH_AMOUNT");
                    assertThat(response.ruleVersion()).isEqualTo("risk-rules-v1");
                })
                .verifyComplete();
    }

    @Test
    void scoreMapsUnspecifiedDecisionToUnavailableOutcome() {
        respondWith(ScorePaymentResponse.newBuilder()
                .setDecision(RiskDecision.RISK_DECISION_UNSPECIFIED)
                .setScore(0)
                .addReasonCodes(RiskReasonCode.RISK_REASON_CODE_UNSPECIFIED)
                .setRuleVersion("risk-rules-v1")
                .build());

        StepVerifier.create(client.score(validRequest()))
                .assertNext(response ->
                        assertThat(response.outcome()).isEqualTo(RiskScoringOutcome.UNAVAILABLE)
                )
                .verifyComplete();
    }

    @Test
    void scoreMapsDeadlineExceededToTimeoutResponse() {
        failWith(Status.DEADLINE_EXCEEDED.asRuntimeException());

        StepVerifier.create(client.score(validRequest()))
                .assertNext(response -> {
                    assertThat(response.outcome()).isEqualTo(RiskScoringOutcome.TIMEOUT);
                    assertThat(response.reasonCodes()).containsExactly("RISK_SERVICE_TIMEOUT");
                    assertThat(response.ruleVersion()).isEqualTo("unavailable");
                })
                .verifyComplete();
    }

    @Test
    void scoreMapsUnavailableStatusToDownstreamUnavailableResponse() {
        failWith(Status.UNAVAILABLE.asRuntimeException());

        StepVerifier.create(client.score(validRequest()))
                .assertNext(response -> {
                    assertThat(response.outcome()).isEqualTo(RiskScoringOutcome.UNAVAILABLE);
                    assertThat(response.reasonCodes()).containsExactly("DOWNSTREAM_UNAVAILABLE");
                    assertThat(response.ruleVersion()).isEqualTo("unavailable");
                })
                .verifyComplete();
    }

    @Test
    void scoreMapsStatusExceptionUnavailableToDownstreamUnavailableResponse() {
        failWith(Status.UNAVAILABLE.asException());

        StepVerifier.create(client.score(validRequest()))
                .assertNext(response -> {
                    assertThat(response.outcome()).isEqualTo(RiskScoringOutcome.UNAVAILABLE);
                    assertThat(response.reasonCodes()).containsExactly("DOWNSTREAM_UNAVAILABLE");
                })
                .verifyComplete();
    }

    @Test
    void propertiesUseSafeDefaults() {
        RiskGrpcProperties defaultProperties = new RiskGrpcProperties(
                null,
                0,
                null
        );

        assertThat(defaultProperties.host()).isEqualTo("localhost");
        assertThat(defaultProperties.port()).isEqualTo(9090);
        assertThat(defaultProperties.timeout()).isEqualTo(Duration.ofMillis(500));
    }

    @Test
    void propertiesKeepConfiguredValues() {
        RiskGrpcProperties configuredProperties = new RiskGrpcProperties(
                "risk-service",
                9091,
                Duration.ofSeconds(2)
        );

        assertThat(configuredProperties.host()).isEqualTo("risk-service");
        assertThat(configuredProperties.port()).isEqualTo(9091);
        assertThat(configuredProperties.timeout()).isEqualTo(Duration.ofSeconds(2));
    }

    private void respondWith(ScorePaymentResponse response) {
        doAnswer(invocation -> {
            StreamObserver<ScorePaymentResponse> observer = invocation.getArgument(1);
            observer.onNext(response);
            observer.onCompleted();
            return null;
        }).when(stub).scorePayment(any(ScorePaymentRequest.class), any(StreamObserver.class));
    }

    private void failWith(StatusRuntimeException exception) {
        failWithStatus(exception);
    }

    private void failWith(StatusException exception) {
        failWithStatus(exception);
    }

    private void failWithStatus(Exception exception) {
        doAnswer(invocation -> {
            StreamObserver<ScorePaymentResponse> observer = invocation.getArgument(1);
            observer.onError(exception);
            return null;
        }).when(stub).scorePayment(any(ScorePaymentRequest.class), any(StreamObserver.class));
    }

    private static RiskScoringRequest validRequest() {
        return new RiskScoringRequest(
                "pay_test_123",
                10_000,
                "USD",
                "merchant_123",
                "customer_123",
                "device_123",
                "corr_123"
        );
    }
}
