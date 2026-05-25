package dev.kavrin.paymentrisk.payment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Public JSON response body returned after a payment authorization attempt.")
public record AuthorizationResponse(

        @Schema(example = "pay_01HX7R0BYV9Y6CNW3HZ7R8E4P2")
        String paymentId,

        @Schema(
                example = "AUTHORIZED",
                allowableValues = {
                        "RECEIVED",
                        "RISK_PENDING",
                        "RISK_APPROVED",
                        "AUTHORIZED",
                        "DECLINED",
                        "REVERSED",
                        "FAILED"
                }
        )
        String status,

        @Schema(example = "AUTH-7F3K9Q", nullable = true)
        String authorizationCode,

        @Schema(
                example = "APPROVED",
                allowableValues = {
                        "APPROVED",
                        "DECLINED",
                        "REVIEW_REQUIRED"
                }
        )
        String riskDecision,

        @Schema(example = "[\"LOW_RISK\", \"KNOWN_DEVICE\"]")
        List<String> reasonCodes,

        @Schema(example = "corr_01HX7R2HBK51S6ZGJ7FN9K4M8D")
        String correlationId,

        @Schema(example = "18", minimum = "0", maximum = "100")
        Integer riskScore,

        @Schema(example = "risk-rules-v1")
        String ruleVersion,

        @Schema(example = "2026-05-25T10:15:30Z")
        Instant createdAt
) {
}