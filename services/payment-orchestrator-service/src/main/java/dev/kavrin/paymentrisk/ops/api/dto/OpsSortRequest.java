package dev.kavrin.paymentrisk.ops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Stable sort query parameters for operations API list endpoints.")
public record OpsSortRequest(

        @Schema(example = DEFAULT_SORT_BY)
        @Size(max = 80, message = "sortBy must be at most 80 characters")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9.]*$", message = "sortBy must be a field path")
        String sortBy,

        @Schema(example = "DESC")
        OpsSortDirection sortDirection
) {
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final OpsSortDirection DEFAULT_SORT_DIRECTION = OpsSortDirection.DESC;

    public String resolvedSortBy() {
        if (sortBy == null || sortBy.isBlank()) {
            return DEFAULT_SORT_BY;
        }

        return sortBy;
    }

    public OpsSortDirection resolvedSortDirection() {
        if (sortDirection == null) {
            return DEFAULT_SORT_DIRECTION;
        }

        return sortDirection;
    }
}
