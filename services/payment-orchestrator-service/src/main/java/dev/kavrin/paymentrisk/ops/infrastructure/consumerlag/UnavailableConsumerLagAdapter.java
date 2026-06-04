package dev.kavrin.paymentrisk.ops.infrastructure.consumerlag;

import dev.kavrin.paymentrisk.ops.application.consumerlag.ConsumerLagPort;
import dev.kavrin.paymentrisk.ops.application.consumerlag.ConsumerLagRequest;
import dev.kavrin.paymentrisk.ops.application.consumerlag.ConsumerLagResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UnavailableConsumerLagAdapter implements ConsumerLagPort {

    @Override
    public Mono<ConsumerLagResult> inspect(ConsumerLagRequest request) {
        return Mono.just(ConsumerLagResult.unavailable(
                "Kafka consumer lag inspection is not configured yet."
        ));
    }
}
