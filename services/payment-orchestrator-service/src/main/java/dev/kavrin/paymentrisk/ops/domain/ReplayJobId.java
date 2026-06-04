package dev.kavrin.paymentrisk.ops.domain;

public record ReplayJobId(String value) {

    public ReplayJobId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("replay job id is required");
        }
        value = value.trim();
    }

    public static ReplayJobId of(String value) {
        return new ReplayJobId(value);
    }
}