package dev.kavrin.paymentrisk.consumer.infrastructure.persistence;

import dev.kavrin.paymentrisk.consumer.application.KafkaDeadLetterRecorder;
import dev.kavrin.paymentrisk.shared.messaging.MessagingObservability;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Stores poison Kafka records in the dead-letter table for operator inspection.
 */
@Repository
@RequiredArgsConstructor
public class DatabaseKafkaDeadLetterRecorder implements KafkaDeadLetterRecorder {

    private static final int MAX_ERROR_LENGTH = 1000;
    private static final int MAX_PAYLOAD_PREVIEW_LENGTH = 4000;

    private final DatabaseClient databaseClient;
    private final MessagingObservability observability;

    @Override
    public Mono<Void> record(
            String consumerName,
            ConsumerRecord<String, String> record,
            Throwable error
    ) {
        var now = Instant.now();
        var deadLetterId = "dlq_" + UUID.randomUUID();
        var eventId = headerOrNull(record, "event_id");
        var correlationId = headerOrNull(record, "correlation_id");
        var failureReason = truncate(error.getClass().getSimpleName() + ": " + error.getMessage(), MAX_ERROR_LENGTH);
        var payloadPreview = truncate(record.value(), MAX_PAYLOAD_PREVIEW_LENGTH);
        var headersJson = headersJson(record);

        var insertDeadLetter = bindNullable(bindNullable(databaseClient.sql("""
                                        INSERT INTO dead_letter_records (
                                            dead_letter_id,
                                            source_system,
                                            destination_name,
                                            kafka_partition,
                                            kafka_offset,
                                            event_id,
                                            message_id,
                                            status,
                                            failure_reason,
                                            failed_at,
                                            replay_eligible,
                                            correlation_id,
                                            headers_json,
                                            payload_preview,
                                            created_at
                                        )
                                        VALUES (
                                            :deadLetterId,
                                            'KAFKA',
                                            :destinationName,
                                            :kafkaPartition,
                                            :kafkaOffset,
                                            :eventId,
                                            :messageId,
                                            'RECORDED',
                                            :failureReason,
                                            :failedAt,
                                            true,
                                            :correlationId,
                                            :headersJson,
                                            :payloadPreview,
                                            :createdAt
                                        )
                                        """)
                                .bind("deadLetterId", deadLetterId)
                                .bind("destinationName", record.topic())
                                .bind("kafkaPartition", record.partition())
                                .bind("kafkaOffset", record.offset())
                                .bind("messageId", safe(record.key()))
                                .bind("failureReason", failureReason)
                                .bind("failedAt", now)
                                .bind("headersJson", headersJson)
                                .bind("payloadPreview", payloadPreview)
                                .bind("createdAt", now),
                        "eventId",
                        eventId,
                        String.class
                ),
                "correlationId",
                correlationId,
                String.class
        );

        return insertDeadLetter.fetch()
                .rowsUpdated()
                .then(writeDeadLetterRecordedOutbox(
                        deadLetterId,
                        record,
                        eventId,
                        correlationId,
                        failureReason,
                        headersJson,
                        payloadPreview,
                        now
                ))
                .doOnSuccess(ignored -> observability.recordDeadLetter("KAFKA"))
                .then();
    }

    private Mono<Long> writeDeadLetterRecordedOutbox(
            String deadLetterId,
            ConsumerRecord<String, String> record,
            String eventId,
            String correlationId,
            String failureReason,
            String headersJson,
            String payloadPreview,
            Instant now
    ) {
        var outboxEventId = "evt_" + deadLetterId;

        return databaseClient.sql("""
                        INSERT INTO outbox_events (
                            event_id,
                            aggregate_type,
                            aggregate_id,
                            event_type,
                            schema_version,
                            producer,
                            correlation_id,
                            payload_json,
                            status,
                            retry_count,
                            occurred_at,
                            created_at
                        )
                        VALUES (
                            :eventId,
                            'DEAD_LETTER',
                            :aggregateId,
                            'DeadLetterRecorded',
                            '1',
                            'payment-orchestrator-service',
                            :correlationId,
                            :payloadJson,
                            'PENDING',
                            0,
                            :occurredAt,
                            :createdAt
                        )
                        """)
                .bind("eventId", outboxEventId)
                .bind("aggregateId", safe(firstPresent(eventId, record.key(), deadLetterId)))
                .bind("correlationId", safe(firstPresent(correlationId, "corr_" + deadLetterId)))
                .bind("payloadJson", deadLetterPayload(deadLetterId, record, eventId, correlationId, failureReason, headersJson, payloadPreview))
                .bind("occurredAt", now)
                .bind("createdAt", now)
                .fetch()
                .rowsUpdated();
    }

    private static String headerOrNull(ConsumerRecord<String, String> record, String name) {
        var header = record.headers().lastHeader(name);

        if (header == null || header.value() == null) {
            return null;
        }

        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static <T> GenericExecuteSpec bindNullable(
            GenericExecuteSpec spec,
            String name,
            T value,
            Class<T> type
    ) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }

    private static String headersJson(ConsumerRecord<String, String> record) {
        var json = new StringBuilder("{");
        var first = true;

        for (var header : record.headers()) {
            if (!first) {
                json.append(',');
            }

            first = false;
            json.append('"')
                    .append(escape(header.key()))
                    .append("\":\"")
                    .append(header.value() == null ? "" : HexFormat.of().formatHex(header.value()))
                    .append('"');
        }

        return json.append('}').toString();
    }

    private static String deadLetterPayload(
            String deadLetterId,
            ConsumerRecord<String, String> record,
            String eventId,
            String correlationId,
            String failureReason,
            String headersJson,
            String payloadPreview
    ) {
        return """
                {"deadLetterId":"%s","sourceSystem":"KAFKA","destinationName":"%s","partition":%d,"offset":%d,"eventId":"%s","messageId":"%s","correlationId":"%s","failureReason":"%s","headers":%s,"payloadPreview":"%s"}
                """.formatted(
                escape(deadLetterId),
                escape(record.topic()),
                record.partition(),
                record.offset(),
                escape(safe(eventId)),
                escape(safe(record.key())),
                escape(safe(correlationId)),
                escape(failureReason),
                headersJson,
                escape(payloadPreview)
        );
    }

    private static String firstPresent(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private static String firstPresent(String first, String second, String third) {
        return firstPresent(firstPresent(first, second), third);
    }

    private static String escape(String value) {
        return safe(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
