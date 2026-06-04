package dev.kavrin.paymentrisk.ops.infrastructure.persistence;

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
@Table("dead_letter_records")
public class DeadLetterRecordEntity {

    @Id
    @Column("dead_letter_id")
    private String deadLetterId;

    @Column("source_system")
    private String sourceSystem;

    @Column("destination_name")
    private String destinationName;

    @Column("kafka_partition")
    private Integer kafkaPartition;

    @Column("kafka_offset")
    private Long kafkaOffset;

    @Column("delivery_tag")
    private String deliveryTag;

    @Column("event_id")
    private String eventId;

    @Column("message_id")
    private String messageId;

    @Column("status")
    private String status;

    @Column("failure_reason")
    private String failureReason;

    @Column("failed_at")
    private Instant failedAt;

    @Column("replay_eligible")
    private boolean replayEligible;

    @Column("correlation_id")
    private String correlationId;

    @Column("payload_preview")
    private String payloadPreview;

    @Column("created_at")
    private Instant createdAt;
}
