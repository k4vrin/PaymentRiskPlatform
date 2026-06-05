package dev.kavrin.paymentrisk.consumer.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class IdempotentConsumerGuard {

    private final ProcessedMessageStore processedMessageStore;
    private final TransactionalOperator transactionalOperator;

    public Mono<Boolean> processOnce(
            ProcessedMessageCommand command,
            Mono<Void> processing
    ) {
        return processedMessageStore.isProcessed(command.consumerName(), command.eventId())
                .flatMap(alreadyProcessed -> {
                    if (alreadyProcessed) {
                        return Mono.just(false);
                    }

                    return processedMessageStore.recordProcessed(command)
                            .flatMap(recorded -> {
                                if (!recorded) {
                                    return Mono.just(false);
                                }

                                // The marker and projection writes must share one transaction:
                                // if projection fails, the marker rolls back and Kafka can retry.
                                return processing.thenReturn(true);
                            })
                            .as(transactionalOperator::transactional);
                });
    }
}
