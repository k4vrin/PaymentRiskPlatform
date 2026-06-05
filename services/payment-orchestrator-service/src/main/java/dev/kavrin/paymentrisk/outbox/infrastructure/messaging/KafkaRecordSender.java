package dev.kavrin.paymentrisk.outbox.infrastructure.messaging;

import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.core.publisher.Mono;

public interface KafkaRecordSender {

    Mono<Void> send(ProducerRecord<String, String> record);
}