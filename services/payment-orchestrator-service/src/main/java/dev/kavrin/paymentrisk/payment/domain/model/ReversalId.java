package dev.kavrin.paymentrisk.payment.domain.model;

import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;

public record ReversalId(String value) {

    private static final PlatformIdGeneratorFactory ID_GENERATOR = new PlatformIdGeneratorFactory();

    public ReversalId {
        value = RequiredText.require(value, "reversalId", 100);
    }

    public static ReversalId newId() {
        return new ReversalId(
                ID_GENERATOR.reversalId()
        );
    }

    public static ReversalId of(String value) {
        return new ReversalId(value);
    }
}