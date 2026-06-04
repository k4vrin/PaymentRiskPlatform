package dev.kavrin.paymentrisk.ops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Schema(description = "Stable pagination query parameters for operations API list endpoints.")
public record OpsPageRequest(

        @Schema(example = "50", minimum = "1", maximum = "100")
        @Min(value = 1, message = "size must be at least 1")
        @Max(value = MAX_SIZE, message = "size must be at most 100")
        Integer size,

        @Schema(example = "eyJjcmVhdGVkQXQiOiIyMDI2LTA2LTA0VDEwOjAwOjAwWiJ9")
        @Size(max = 512, message = "pageToken must be at most 512 characters")
        String pageToken
) {
    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 100;

    public int resolvedSize() {
        if (size == null) {
            return DEFAULT_SIZE;
        }

        return size;
    }
}
