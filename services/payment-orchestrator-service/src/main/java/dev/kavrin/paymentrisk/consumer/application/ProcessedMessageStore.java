package dev.kavrin.paymentrisk.consumer.application;

import reactor.core.publisher.Mono;

public interface ProcessedMessageStore {

    Mono<Boolean> isProcessed(String consumerName, String eventId);

    Mono<Boolean> recordProcessed(ProcessedMessageCommand command);
}
