package dev.kavrin.paymentrisk.consumer.infrastructure.persistence;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("processed_kafka_messages")
public class ProcessedKafkaMessageEntity {

    @Id
    @Column("processed_message_id")
    private String processedMessageId;

    @Column("consumer_name")
    private String consumerName;

    @Column("topic")
    private String topic;

    @Column("partition_id")
    private int partition;

    @Column("message_offset")
    private long offset;

    @Column("event_id")
    private String eventId;

    @Column("processed_at")
    private Instant processedAt;
}
