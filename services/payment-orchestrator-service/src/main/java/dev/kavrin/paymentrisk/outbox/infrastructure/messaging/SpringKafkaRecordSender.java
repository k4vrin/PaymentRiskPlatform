package dev.kavrin.paymentrisk.outbox.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(prefix = "payment-risk.outbox.relay", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class SpringKafkaRecordSender implements KafkaRecordSender {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public Mono<Void> send(ProducerRecord<String, String> record) {
        // Mono.fromFuture bridges Java async APIs into Reactor.
        return Mono.fromFuture(() -> kafkaTemplate.send(record))
                .then();
    }
}
