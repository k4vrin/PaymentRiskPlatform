package dev.kavrin.paymentrisk.risk.application;

import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringOutcome;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringRequest;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskScoringClientTest {

    @Test
    void fakeRiskClientCanReturnApprovedResult() {
        RiskScoringClient client = request -> Mono.just(
                RiskScoringResponse.approved(
                        12,
                        List.of("LOW_RISK"),
                        "test-rules-v1"
                )
        );

        StepVerifier.create(client.score(validRequest()))
                .assertNext(response -> {
                    assertThat(response.outcome()).isEqualTo(RiskScoringOutcome.APPROVED);
                    assertThat(response.score()).isEqualTo(12);
                    assertThat(response.reasonCodes()).containsExactly("LOW_RISK");
                    assertThat(response.ruleVersion()).isEqualTo("test-rules-v1");
                })
                .verifyComplete();
    }

    @Test
    void fakeRiskClientCanReturnDeclinedResult() {
        RiskScoringClient client = request -> Mono.just(
                RiskScoringResponse.declined(
                        95,
                        List.of("HIGH_AMOUNT"),
                        "test-rules-v1"
                )
        );

        StepVerifier.create(client.score(validRequest()))
                .assertNext(response -> {
                    assertThat(response.outcome()).isEqualTo(RiskScoringOutcome.DECLINED);
                    assertThat(response.score()).isEqualTo(95);
                    assertThat(response.reasonCodes()).containsExactly("HIGH_AMOUNT");
                })
                .verifyComplete();
    }

    @Test
    void fakeRiskClientCanReturnReviewRequiredResult() {
        RiskScoringClient client = request -> Mono.just(
                RiskScoringResponse.reviewRequired(
                        61,
                        List.of("MANUAL_REVIEW_REQUIRED"),
                        "test-rules-v1"
                )
        );

        StepVerifier.create(client.score(validRequest()))
                .assertNext(response -> {
                    assertThat(response.outcome()).isEqualTo(RiskScoringOutcome.REVIEW_REQUIRED);
                    assertThat(response.score()).isEqualTo(61);
                    assertThat(response.reasonCodes()).containsExactly("MANUAL_REVIEW_REQUIRED");
                    assertThat(response.ruleVersion()).isEqualTo("test-rules-v1");
                })
                .verifyComplete();
    }

    @Test
    void fakeRiskClientCanReturnTimeoutResult() {
        RiskScoringClient client = request -> Mono.just(RiskScoringResponse.timeout());

        StepVerifier.create(client.score(validRequest()))
                .assertNext(response -> {
                    assertThat(response.outcome()).isEqualTo(RiskScoringOutcome.TIMEOUT);
                    assertThat(response.score()).isZero();
                    assertThat(response.reasonCodes()).containsExactly("RISK_SERVICE_TIMEOUT");
                    assertThat(response.ruleVersion()).isEqualTo("unavailable");
                })
                .verifyComplete();
    }

    @Test
    void fakeRiskClientCanReturnUnavailableResult() {
        RiskScoringClient client = request -> Mono.just(RiskScoringResponse.unavailable());

        StepVerifier.create(client.score(validRequest()))
                .assertNext(response -> {
                    assertThat(response.outcome()).isEqualTo(RiskScoringOutcome.UNAVAILABLE);
                    assertThat(response.score()).isZero();
                    assertThat(response.reasonCodes()).containsExactly("DOWNSTREAM_UNAVAILABLE");
                    assertThat(response.ruleVersion()).isEqualTo("unavailable");
                })
                .verifyComplete();
    }

    @Test
    void requestRejectsInvalidRequiredFields() {
        assertThatThrownBy(() -> new RiskScoringRequest(
                " ",
                10_000,
                "USD",
                "merchant_123",
                "customer_123",
                "device_123",
                "corr_123"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("paymentId must not be blank");

        assertThatThrownBy(() -> new RiskScoringRequest(
                "pay_test_123",
                0,
                "USD",
                "merchant_123",
                "customer_123",
                "device_123",
                "corr_123"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amountMinor must be positive");
    }

    @Test
    void responseDefensivelyCopiesReasonCodes() {
        List<String> reasonCodes = new java.util.ArrayList<>();
        reasonCodes.add("LOW_RISK");

        RiskScoringResponse response = RiskScoringResponse.approved(
                12,
                reasonCodes,
                "test-rules-v1"
        );
        reasonCodes.add("MUTATED");

        assertThat(response.reasonCodes()).containsExactly("LOW_RISK");
        assertThatThrownBy(() -> response.reasonCodes().add("OTHER"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void responseRejectsInvalidRequiredFields() {
        assertThatThrownBy(() -> RiskScoringResponse.approved(
                -1,
                List.of("LOW_RISK"),
                "test-rules-v1"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("score must not be negative");

        assertThatThrownBy(() -> RiskScoringResponse.approved(
                12,
                List.of(" "),
                "test-rules-v1"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("reasonCode must not be blank");
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
