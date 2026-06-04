package dev.kavrin.paymentrisk.ops.application.consumerlag;

import reactor.core.publisher.Mono;

public interface ConsumerLagService {

    Mono<ConsumerLagResult> inspect(ConsumerLagRequest request);
}
