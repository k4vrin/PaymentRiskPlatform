package dev.kavrin.paymentrisk.ops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Stable paginated response shape for operations API list endpoints.")
public record OpsPageResponse<T>(
        List<T> items,
        PageMetadata page
) {
    public OpsPageResponse {
        items = List.copyOf(items);
    }

    public record PageMetadata(
            int size,
            String nextPageToken,
            boolean hasNext
    ) {
    }
}
