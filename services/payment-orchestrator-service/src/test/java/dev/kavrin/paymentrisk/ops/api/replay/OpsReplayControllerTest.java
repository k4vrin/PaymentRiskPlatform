package dev.kavrin.paymentrisk.ops.api.replay;

import dev.kavrin.paymentrisk.ops.application.replay.ReplayJobResult;
import dev.kavrin.paymentrisk.ops.application.replay.ReplayRequestCommand;
import dev.kavrin.paymentrisk.ops.application.replay.ReplayRequestService;
import dev.kavrin.paymentrisk.ops.domain.ReplayJobStatus;
import dev.kavrin.paymentrisk.ops.domain.ReplaySource;
import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIdWebFilter;
import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIds;
import dev.kavrin.paymentrisk.shared.api.error.ApiErrorCode;
import dev.kavrin.paymentrisk.shared.api.error.ConflictException;
import dev.kavrin.paymentrisk.shared.api.error.GlobalApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@WebFluxTest(
        controllers = OpsReplayController.class,
        excludeAutoConfiguration = ReactiveWebSecurityAutoConfiguration.class
)
@Import({
        CorrelationIdWebFilter.class,
        GlobalApiExceptionHandler.class,
        OpsReplayControllerTest.TestReplayConfiguration.class
})
class OpsReplayControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CapturingReplayRequestService replayRequestService;

    @BeforeEach
    void resetService() {
        replayRequestService.reset();
    }

    @Test
    void requestReplayMapsRequestAndReturnsJob() {
        webTestClient.post()
                .uri("/api/v1/ops/replay/OUTBOX/evt_001")
                .header(CorrelationIds.HEADER_NAME, "corr_replay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"reason\":\"manual retry\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.replayJobId").isEqualTo("replay_001")
                .jsonPath("$.targetId").isEqualTo("evt_001")
                .jsonPath("$.source").isEqualTo("OUTBOX")
                .jsonPath("$.requestedBy").isEqualTo("anonymous")
                .jsonPath("$.reason").isEqualTo("manual retry")
                .jsonPath("$.correlationId").isEqualTo("corr_replay");

        assertThat(replayRequestService.lastCommand.get()).isNotNull();
        assertThat(replayRequestService.lastCommand.get().source()).isEqualTo(ReplaySource.OUTBOX);
        assertThat(replayRequestService.lastCommand.get().targetId()).isEqualTo("evt_001");
        assertThat(replayRequestService.lastCommand.get().correlationId()).isEqualTo("corr_replay");
    }

    @Test
    void requestReplayReturnsStructuredConflict() {
        replayRequestService.error = new ConflictException(
                ApiErrorCode.Business.OUTBOX_EVENT_NOT_REPLAYABLE,
                "Replay target is not replayable."
        );

        webTestClient.post()
                .uri("/api/v1/ops/replay/OUTBOX/evt_001")
                .header(CorrelationIds.HEADER_NAME, "corr_replay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("OUTBOX_EVENT_NOT_REPLAYABLE")
                .jsonPath("$.correlationId").isEqualTo("corr_replay");
    }

    @TestConfiguration
    static class TestReplayConfiguration {
        @Bean
        CapturingReplayRequestService replayRequestService() {
            return new CapturingReplayRequestService();
        }
    }

    static class CapturingReplayRequestService implements ReplayRequestService {
        private final AtomicReference<ReplayRequestCommand> lastCommand = new AtomicReference<>();
        private RuntimeException error;

        void reset() {
            lastCommand.set(null);
            error = null;
        }

        @Override
        public Mono<ReplayJobResult> requestReplay(ReplayRequestCommand command) {
            lastCommand.set(command);
            if (error != null) {
                return Mono.error(error);
            }
            return Mono.just(new ReplayJobResult(
                    "replay_001",
                    command.targetId(),
                    command.source(),
                    command.requestedBy(),
                    Instant.parse("2026-06-04T10:00:00Z"),
                    ReplayJobStatus.REQUESTED,
                    command.reason(),
                    Optional.empty(),
                    command.correlationId()
            ));
        }
    }
}
