package dev.kavrin.paymentrisk.shared.api.contract;

import dev.kavrin.paymentrisk.risk.v1.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskGrpcContractTest {

    @Test
    void scorePaymentRequestCanBeConstructedFromGeneratedJavaContract() {
        ScorePaymentRequest request = ScorePaymentRequest.newBuilder()
                .setPaymentId("pay_123")
                .setAmountMinor(1299)
                .setCurrency("USD")
                .setMerchantId("mer_123")
                .setCustomerId("cus_123")
                .setDeviceFingerprint("device_123")
                .setCorrelationId("corr_123")
                .build();

        assertThat(request.getPaymentId()).isEqualTo("pay_123");
        assertThat(request.getAmountMinor()).isEqualTo(1299);
        assertThat(request.getCorrelationId()).isEqualTo("corr_123");
    }

    @Test
    void scorePaymentResponseCanBeConstructedFromGeneratedJavaContract() {
        ScorePaymentResponse response = ScorePaymentResponse.newBuilder()
                .setScore(42)
                .setDecision(RiskDecision.RISK_DECISION_APPROVED)
                .addReasonCodes(RiskReasonCode.RISK_REASON_CODE_LOW_RISK_PAYMENT)
                .addRuleHits(RiskRuleHit.newBuilder()
                        .setRuleId("LOW_RISK_RULE")
                        .setReasonCode(RiskReasonCode.RISK_REASON_CODE_LOW_RISK_PAYMENT)
                        .setScoreDelta(-10)
                        .setMessage("Payment is within low-risk thresholds."))
                .setRuleVersion("local-v1")
                .build();

        assertThat(response.getDecision()).isEqualTo(RiskDecision.RISK_DECISION_APPROVED);
        assertThat(response.getReasonCodesList()).containsExactly(RiskReasonCode.RISK_REASON_CODE_LOW_RISK_PAYMENT);
        assertThat(response.getRuleHitsList()).hasSize(1);
    }
}
