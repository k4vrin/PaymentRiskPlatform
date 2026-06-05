package dev.kavrin.paymentrisk.consumer.application;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IdempotentConsumerGuardTest {

    private final InMemoryProcessedMessageStore store = new InMemoryProcessedMessageStore();
    private final TransactionalOperator transactionalOperator = mock(TransactionalOperator.class);
    private final IdempotentConsumerGuard guard = new IdempotentConsumerGuard(
            store,
            transactionalOperator
    );

    IdempotentConsumerGuardTest() {
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldProcessAndRecordNewMessage() {
        var sideEffects = new ArrayList<String>();

        StepVerifier.create(guard.processOnce(
                        command("evt_1"),
                        Mono.fromRunnable(() -> sideEffects.add("projected"))
                ))
                .expectNext(true)
                .verifyComplete();

        assertThat(sideEffects).containsExactly("projected");
        assertThat(store.recorded).containsExactly("payments-audit:evt_1");
        assertThat(store.operations).containsExactly("exists:evt_1", "record:evt_1");
        verify(transactionalOperator).transactional(any(Mono.class));
    }

    @Test
    void shouldSkipAlreadyProcessedMessage() {
        store.processed.add("payments-audit:evt_1");
        var sideEffects = new ArrayList<String>();

        StepVerifier.create(guard.processOnce(
                        command("evt_1"),
                        Mono.fromRunnable(() -> sideEffects.add("projected"))
                ))
                .expectNext(false)
                .verifyComplete();

        assertThat(sideEffects).isEmpty();
        assertThat(store.recorded).isEmpty();
        assertThat(store.operations).containsExactly("exists:evt_1");
        verify(transactionalOperator, never()).transactional(any(Mono.class));
    }

    @Test
    void shouldSkipProcessingWhenConcurrentConsumerAlreadyRecordedMessage() {
        store.rejectRecord = true;
        var sideEffects = new ArrayList<String>();

        StepVerifier.create(guard.processOnce(
                        command("evt_1"),
                        Mono.fromRunnable(() -> sideEffects.add("projected"))
                ))
                .expectNext(false)
                .verifyComplete();

        assertThat(sideEffects).isEmpty();
        assertThat(store.recorded).isEmpty();
        assertThat(store.operations).containsExactly("exists:evt_1", "record:evt_1");
        verify(transactionalOperator).transactional(any(Mono.class));
    }

    private static ProcessedMessageCommand command(String eventId) {
        return new ProcessedMessageCommand(
                "payments-audit",
                "payment.authorization.completed",
                0,
                42,
                eventId
        );
    }

    private static final class InMemoryProcessedMessageStore implements ProcessedMessageStore {

        private final Set<String> processed = new HashSet<>();
        private final List<String> recorded = new ArrayList<>();
        private final List<String> operations = new ArrayList<>();
        private boolean rejectRecord;

        @Override
        public Mono<Boolean> isProcessed(String consumerName, String eventId) {
            operations.add("exists:" + eventId);
            return Mono.just(processed.contains(key(consumerName, eventId)));
        }

        @Override
        public Mono<Boolean> recordProcessed(ProcessedMessageCommand command) {
            operations.add("record:" + command.eventId());
            if (rejectRecord) {
                return Mono.just(false);
            }

            var key = key(command.consumerName(), command.eventId());
            processed.add(key);
            recorded.add(key);
            return Mono.just(true);
        }

        private static String key(String consumerName, String eventId) {
            return consumerName + ":" + eventId;
        }
    }
}
