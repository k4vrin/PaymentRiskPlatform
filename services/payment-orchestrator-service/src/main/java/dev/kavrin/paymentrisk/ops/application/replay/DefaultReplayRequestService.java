package dev.kavrin.paymentrisk.ops.application.replay;

import dev.kavrin.paymentrisk.shared.api.error.ApiErrorCode;
import dev.kavrin.paymentrisk.shared.api.error.ConflictException;
import dev.kavrin.paymentrisk.shared.api.error.ResourceNotFoundException;
import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DefaultReplayRequestService implements ReplayRequestService {

    private final ReplayTargetLookupPort targetLookupPort;
    private final ReplayJobStore replayJobStore;
    private final ReplayAuditPort replayAuditPort;
    private final PlatformIdGeneratorFactory idGeneratorFactory;

    @Override
    public Mono<ReplayJobResult> requestReplay(ReplayRequestCommand command) {
        return targetLookupPort.findTarget(command.source(), command.targetId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Replay target was not found.")))
                .flatMap(target -> {
                    if (!target.replayable()) {
                        return Mono.error(new ConflictException(
                                ApiErrorCode.Business.OUTBOX_EVENT_NOT_REPLAYABLE,
                                "Replay target is not replayable from status " + target.status() + "."
                        ));
                    }

                    return replayJobStore.hasActiveReplay(command.source().name(), command.targetId())
                            .flatMap(active -> active
                                    ? Mono.error(new ConflictException(
                                    ApiErrorCode.Business.OUTBOX_EVENT_NOT_REPLAYABLE,
                                    "An active replay job already exists for this target."
                            ))
                                    : replayJobStore.saveRequested(command, idGeneratorFactory.replayJobId())
                                      .flatMap(job -> replayAuditPort.recordReplayRequested(job).thenReturn(job)));
                });
    }
}
