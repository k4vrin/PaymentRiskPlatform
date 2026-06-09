package dev.kavrin.paymentrisk.ops.application.replay;

import dev.kavrin.paymentrisk.ops.domain.ReplaySource;
import dev.kavrin.paymentrisk.shared.api.error.ConflictException;
import dev.kavrin.paymentrisk.shared.api.error.ResourceNotFoundException;
import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;
import dev.kavrin.paymentrisk.shared.messaging.MessagingObservability;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultReplayRequestServiceTest {

    private FakeReplayTargetLookupPort targetLookupPort;
    private FakeReplayJobStore replayJobStore;
    private FakeReplayAuditPort replayAuditPort;
    private SimpleMeterRegistry meterRegistry;
    private DefaultReplayRequestService service;

    @BeforeEach
    void setUp() {
        targetLookupPort = new FakeReplayTargetLookupPort();
        replayJobStore = new FakeReplayJobStore();
        replayAuditPort = new FakeReplayAuditPort();
        meterRegistry = new SimpleMeterRegistry();
        service = new DefaultReplayRequestService(
                targetLookupPort,
                replayJobStore,
                replayAuditPort,
                new FixedIdGeneratorFactory(),
                new MessagingObservability(meterRegistry)
        );
    }

    @Test
    void createsReplayJobAndAuditWhenTargetIsReplayable() {
        targetLookupPort.target = new ReplayTarget("evt_001", true, "FAILED");

        var result = service.requestReplay(command()).block();

        assertThat(result).isNotNull();
        assertThat(result.replayJobId()).isEqualTo("replay_fixed");
        assertThat(result.targetId()).isEqualTo("evt_001");
        assertThat(result.status().name()).isEqualTo("REQUESTED");
        assertThat(result.reason()).contains("manual retry");
        assertThat(replayAuditPort.lastAudited.get()).isSameAs(result);
        assertThat(replayCounter("success")).isEqualTo(1.0);
    }

    @Test
    void rejectsMissingTarget() {
        targetLookupPort.target = null;

        assertThatThrownBy(() -> service.requestReplay(command()).block())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
        assertThat(replayCounter("failure")).isEqualTo(1.0);
    }

    @Test
    void rejectsNonReplayableTarget() {
        targetLookupPort.target = new ReplayTarget("evt_001", false, "PUBLISHED");

        assertThatThrownBy(() -> service.requestReplay(command()).block())
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not replayable");
        assertThat(replayCounter("failure")).isEqualTo(1.0);
    }

    @Test
    void rejectsDuplicateActiveReplay() {
        targetLookupPort.target = new ReplayTarget("evt_001", true, "FAILED");
        replayJobStore.activeReplay = true;

        assertThatThrownBy(() -> service.requestReplay(command()).block())
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active replay job");
        assertThat(replayCounter("failure")).isEqualTo(1.0);
    }

    private double replayCounter(String result) {
        var counter = meterRegistry.find("payment_risk_replay_requests_total")
                .tag("source", "OUTBOX")
                .tag("result", result)
                .counter();

        return counter == null ? 0.0 : counter.count();
    }

    private ReplayRequestCommand command() {
        return new ReplayRequestCommand(
                ReplaySource.OUTBOX,
                "evt_001",
                "ops-user",
                Optional.of("manual retry"),
                "corr_001"
        );
    }

    private static final class FakeReplayTargetLookupPort implements ReplayTargetLookupPort {
        private ReplayTarget target;

        @Override
        public Mono<ReplayTarget> findTarget(ReplaySource source, String targetId) {
            return target == null ? Mono.empty() : Mono.just(target);
        }
    }

    private static final class FakeReplayJobStore implements ReplayJobStore {
        private boolean activeReplay;

        @Override
        public Mono<Boolean> hasActiveReplay(String source, String targetId) {
            return Mono.just(activeReplay);
        }

        @Override
        public Mono<ReplayJobResult> saveRequested(ReplayRequestCommand command, String replayJobId) {
            return Mono.just(new ReplayJobResult(
                    replayJobId,
                    command.targetId(),
                    command.source(),
                    command.requestedBy(),
                    Instant.parse("2026-06-04T10:00:00Z"),
                    dev.kavrin.paymentrisk.ops.domain.ReplayJobStatus.REQUESTED,
                    command.reason(),
                    Optional.empty(),
                    command.correlationId()
            ));
        }
    }

    private static final class FakeReplayAuditPort implements ReplayAuditPort {
        private final AtomicReference<ReplayJobResult> lastAudited = new AtomicReference<>();

        @Override
        public Mono<Void> recordReplayRequested(ReplayJobResult replayJob) {
            lastAudited.set(replayJob);
            return Mono.empty();
        }
    }

    private static final class FixedIdGeneratorFactory extends PlatformIdGeneratorFactory {
        @Override
        public String replayJobId() {
            return "replay_fixed";
        }
    }
}
