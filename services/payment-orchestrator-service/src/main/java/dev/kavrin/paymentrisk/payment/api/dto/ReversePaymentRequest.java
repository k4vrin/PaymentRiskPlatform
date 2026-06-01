package dev.kavrin.paymentrisk.payment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Public JSON request body for reversing an authorized payment.")
public record ReversePaymentRequest(

        @Schema(example = "idem_rev_01HX7QK9JP7E5W5NRZ6T5Q3R1A")
        @NotBlank(message = "idempotencyKey is required")
        @Size(min = 8, max = 128, message = "idempotencyKey must be between 8 and 128 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9._:-]+$",
                message = "idempotencyKey may contain letters, numbers, dot, underscore, colon, and hyphen"
        )
        String idempotencyKey,

        @Schema(example = "merchant_request")
        @Size(max = 500, message = "reason must be at most 500 characters")
        String reason
) {
}
