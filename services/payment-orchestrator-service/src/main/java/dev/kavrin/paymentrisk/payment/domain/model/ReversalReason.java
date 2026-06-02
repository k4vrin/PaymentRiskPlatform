package dev.kavrin.paymentrisk.payment.domain.model;

public record ReversalReason(String value) {

    private static final String DEFAULT_REASON = "merchant_requested";

    public ReversalReason {
        value = RequiredText.require(value, "reversalReason", 500);
    }

    public static ReversalReason of(String value) {
        return new ReversalReason(value);
    }

    public static ReversalReason defaultReason() {
        return new ReversalReason(DEFAULT_REASON);
    }

    public static ReversalReason ofNullable(String value) {
        if (value == null || value.isBlank()) {
            return defaultReason();
        }

        return of(value);
    }
}