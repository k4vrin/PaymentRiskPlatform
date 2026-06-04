package dev.kavrin.paymentrisk.ops.infrastructure.replay.persistence;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,"
                + "org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration,"
                + "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration,"
                + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration,"
                + "org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration,"
                + "org.springframework.boot.security.autoconfigure.actuate.web.reactive.ReactiveManagementWebSecurityAutoConfiguration"
})
@ActiveProfiles("test")
@Import(TestPostgresConfiguration.class)
class ReplayJobEntityRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-06-04T10:00:00Z");

    @Autowired
    private ReplayJobEntityRepository repository;

    @BeforeEach
    void deleteExistingRecords() {
        repository.deleteAll().block();
    }

    @Test
    void savesAndFindsActiveReplayJob() {
        repository.save(replayJob("replay_001", "evt_001", "REQUESTED")).block();

        var active = repository.findFirstBySourceAndTargetIdAndStatusIn(
                        "OUTBOX",
                        "evt_001",
                        List.of("REQUESTED", "RUNNING")
                )
                .block();

        assertThat(active).isNotNull();
        assertThat(active.getReplayJobId()).isEqualTo("replay_001");
        assertThat(active.getRequestedBy()).isEqualTo("ops-user");
    }

    @Test
    void rejectsDuplicateActiveReplayForSameSourceAndTarget() {
        repository.save(replayJob("replay_001", "evt_001", "REQUESTED")).block();

        assertThatThrownBy(() -> repository.save(replayJob("replay_002", "evt_001", "RUNNING")).block())
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void allowsNewReplayAfterTerminalReplayStatus() {
        repository.save(replayJob("replay_001", "evt_001", "FAILED")).block();
        repository.save(replayJob("replay_002", "evt_001", "REQUESTED")).block();

        var jobs = repository.findBySourceAndTargetIdOrderByRequestedAtDesc("OUTBOX", "evt_001")
                .collectList()
                .block();

        assertThat(jobs)
                .isNotNull()
                .extracting(ReplayJobEntity::getReplayJobId)
                .containsExactly("replay_002", "replay_001");
    }

    private ReplayJobEntity replayJob(String replayJobId, String targetId, String status) {
        return ReplayJobEntity.builder()
                .replayJobId(replayJobId)
                .targetId(targetId)
                .source("OUTBOX")
                .requestedBy("ops-user")
                .requestedAt(NOW.plusSeconds(replayJobId.endsWith("002") ? 1 : 0))
                .status(status)
                .reason("manual retry")
                .correlationId("corr_replay")
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
