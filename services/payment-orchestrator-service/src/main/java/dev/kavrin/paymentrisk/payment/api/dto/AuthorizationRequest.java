package dev.kavrin.paymentrisk.payment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Public JSON request body for authorizing a payment.")
public record AuthorizationRequest(

        @Schema(example = "mer_01HX7Q9K2V6M8P4A3B9C1D2E3F")
        @NotBlank(message = "merchantId is required")
        @Size(max = 80)
        String merchantId,

        @Schema(example = "cus_01HX7QAF4CQ8YFZ3M9N2W1P0VK")
        @NotBlank(message = "customerId is required")
        @Size(max = 80)
        String customerId,

        @Schema(example = "1299", minimum = "1")
        @Min(value = 1, message = "amountMinor must be positive")
        long amountMinor,

        @Schema(example = "USD", minLength = 3, maxLength = 3)
        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter uppercase ISO 4217 code")
        String currency,

        @Schema(
                example = "pmt_tok_4f7b8d9c2a1e",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "paymentMethodToken is required")
        @Size(max = 200)
        String paymentMethodToken,

        @Schema(
                example = "dfp_6d9f1a2b3c4e5f678901",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "deviceFingerprint is required")
        @Size(max = 200)
        String deviceFingerprint,

        @Schema(example = "order_2026_000123")
        @Size(max = 120)
        String externalReference,

        @Schema(example = "idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A")
        @NotBlank(message = "idempotencyKey is required")
        @Size(min = 16, max = 120)
        String idempotencyKey
) {
}
