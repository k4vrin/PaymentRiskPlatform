package dev.kavrin.paymentrisk.ops.application.consumerlag;

import java.util.List;
import java.util.Optional;

public record ConsumerLagResult(
        List<ConsumerLagItem> items,
        Optional<String> unavailableReason
) {
    public ConsumerLagResult {
        items = List.copyOf(items);
        unavailableReason = unavailableReason == null ? Optional.empty() : unavailableReason;
    }

    public static ConsumerLagResult unavailable(String reason) {
        return new ConsumerLagResult(List.of(), Optional.of(reason));
    }
}
