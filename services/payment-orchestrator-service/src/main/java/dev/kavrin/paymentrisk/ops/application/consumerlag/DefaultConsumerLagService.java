package dev.kavrin.paymentrisk.ops.application.consumerlag;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DefaultConsumerLagService implements ConsumerLagService {

    private final ConsumerLagPort consumerLagPort;

    @Override
    public Mono<ConsumerLagResult> inspect(ConsumerLagRequest request) {
        return consumerLagPort.inspect(request);
    }
}
