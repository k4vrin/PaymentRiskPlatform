package dev.kavrin.paymentrisk.ops.infrastructure.replay;

import dev.kavrin.paymentrisk.ops.application.replay.ReplayAuditPort;
import dev.kavrin.paymentrisk.ops.application.replay.ReplayJobResult;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.OutboxEventEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.OutboxEventEntityRepository;
import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class OutboxReplayAuditAdapter implements ReplayAuditPort {

    private static final String EVENT_TYPE = "OpsReplayRequested";
    private static final String AGGREGATE_TYPE = "OpsReplayJob";
    private static final String PRODUCER = "payment-orchestrator-service";
    private static final String SCHEMA_VERSION = "1";

    private final OutboxEventEntityRepository outboxEventRepository;
    private final PlatformIdGeneratorFactory idGeneratorFactory;
    private final Clock clock;

    @Override
    public Mono<Void> recordReplayRequested(ReplayJobResult replayJob) {
        var now = Instant.now(clock);
        var payload = """
                {"replayJobId":"%s","targetId":"%s","source":"%s","requestedBy":"%s","requestedAt":"%s","reason":%s}
                """.formatted(
                escape(replayJob.replayJobId()),
                escape(replayJob.targetId()),
                replayJob.source().name(),
                escape(replayJob.requestedBy()),
                replayJob.requestedAt(),
                replayJob.reason().map(value -> "\"" + escape(value) + "\"").orElse("null")
        ).trim();

        var entity = OutboxEventEntity.builder()
                .eventId(idGeneratorFactory.outboxEventId())
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(replayJob.replayJobId())
                .eventType(EVENT_TYPE)
                .schemaVersion(SCHEMA_VERSION)
                .producer(PRODUCER)
                .correlationId(replayJob.correlationId())
                .payloadJson(payload)
                .status("PENDING")
                .retryCount(0)
                .occurredAt(now)
                .createdAt(now)
                .build();

        return outboxEventRepository.save(entity).then();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
