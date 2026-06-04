package dev.kavrin.paymentrisk.ops.application.consumerlag;

import java.util.Optional;

public record ConsumerLagRequest(
        Optional<String> consumerGroup,
        Optional<String> topic
) {
    public ConsumerLagRequest {
        consumerGroup = consumerGroup == null ? Optional.empty() : consumerGroup.map(String::trim).filter(value -> !value.isBlank());
        topic = topic == null ? Optional.empty() : topic.map(String::trim).filter(value -> !value.isBlank());
    }
}
