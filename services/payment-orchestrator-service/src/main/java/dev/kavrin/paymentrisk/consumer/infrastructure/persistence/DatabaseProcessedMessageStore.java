package dev.kavrin.paymentrisk.consumer.infrastructure.persistence;

import dev.kavrin.paymentrisk.consumer.application.ProcessedMessageCommand;
import dev.kavrin.paymentrisk.consumer.application.ProcessedMessageStore;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Clock;

@Repository
@RequiredArgsConstructor
public class DatabaseProcessedMessageStore implements ProcessedMessageStore {

    private final ProcessedKafkaMessageEntityRepository repository;
    private final R2dbcEntityTemplate entityTemplate;
    private final Clock clock;

    @Override
    public Mono<Boolean> isProcessed(String consumerName, String eventId) {
        return repository.existsByConsumerNameAndEventId(consumerName, eventId);
    }

    @Override
    public Mono<Boolean> recordProcessed(ProcessedMessageCommand command) {
        // This table uses an application-assigned ID. Explicit insert keeps the
        // operation atomic: duplicates fail on database constraints and skip work.
        return entityTemplate.insert(ProcessedKafkaMessageEntity.class)
                .using(entity(command))
                .thenReturn(true)
                .onErrorResume(DataIntegrityViolationException.class, ignored -> Mono.just(false));
    }

    private ProcessedKafkaMessageEntity entity(ProcessedMessageCommand command) {
        return ProcessedKafkaMessageEntity.builder()
                .processedMessageId(command.consumerName() + ":" + command.eventId())
                .consumerName(command.consumerName())
                .topic(command.topic())
                .partition(command.partition())
                .offset(command.offset())
                .eventId(command.eventId())
                .processedAt(clock.instant())
                .build();
    }
}
