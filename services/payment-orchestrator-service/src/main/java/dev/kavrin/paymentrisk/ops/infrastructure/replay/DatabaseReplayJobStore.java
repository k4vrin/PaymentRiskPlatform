package dev.kavrin.paymentrisk.ops.infrastructure.replay;

import dev.kavrin.paymentrisk.ops.application.replay.ReplayJobResult;
import dev.kavrin.paymentrisk.ops.application.replay.ReplayJobStore;
import dev.kavrin.paymentrisk.ops.application.replay.ReplayRequestCommand;
import dev.kavrin.paymentrisk.ops.domain.ReplayJobStatus;
import dev.kavrin.paymentrisk.ops.domain.ReplaySource;
import dev.kavrin.paymentrisk.ops.infrastructure.replay.persistence.ReplayJobEntity;
import dev.kavrin.paymentrisk.ops.infrastructure.replay.persistence.ReplayJobEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DatabaseReplayJobStore implements ReplayJobStore {

    private static final List<String> ACTIVE_STATUSES = List.of(
            ReplayJobStatus.REQUESTED.name(),
            ReplayJobStatus.RUNNING.name()
    );

    private final ReplayJobEntityRepository replayJobRepository;
    private final Clock clock;

    @Override
    public Mono<Boolean> hasActiveReplay(String source, String targetId) {
        return replayJobRepository.findFirstBySourceAndTargetIdAndStatusIn(source, targetId, ACTIVE_STATUSES)
                .hasElement();
    }

    @Override
    public Mono<ReplayJobResult> saveRequested(ReplayRequestCommand command, String replayJobId) {
        var now = Instant.now(clock);
        var entity = ReplayJobEntity.builder()
                .replayJobId(replayJobId)
                .targetId(command.targetId())
                .source(command.source().name())
                .requestedBy(command.requestedBy())
                .requestedAt(now)
                .status(ReplayJobStatus.REQUESTED.name())
                .reason(command.reason().orElse(null))
                .correlationId(command.correlationId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return replayJobRepository.save(entity)
                .map(DatabaseReplayJobStore::toResult)
                .onErrorMap(DuplicateKeyException.class, exception ->
                        new IllegalStateException("An active replay job already exists for this target.", exception));
    }

    private static ReplayJobResult toResult(ReplayJobEntity entity) {
        return new ReplayJobResult(
                entity.getReplayJobId(),
                entity.getTargetId(),
                ReplaySource.valueOf(entity.getSource()),
                entity.getRequestedBy(),
                entity.getRequestedAt(),
                ReplayJobStatus.valueOf(entity.getStatus()),
                Optional.ofNullable(entity.getReason()),
                Optional.ofNullable(entity.getFailureReason()),
                entity.getCorrelationId()
        );
    }
}
