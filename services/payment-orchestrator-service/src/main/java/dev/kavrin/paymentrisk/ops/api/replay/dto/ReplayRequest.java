package dev.kavrin.paymentrisk.ops.api.replay.dto;

import jakarta.validation.constraints.Size;

public record ReplayRequest(
        @Size(max = 500)
        String reason
) {
}
