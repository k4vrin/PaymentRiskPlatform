package dev.kavrin.paymentrisk.ops.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReplayJobTest {

    @Test
    void createsRequestedReplayJob() {
        var job = ReplayJob.requested(
                ReplayJobId.of("replay_job_123"),
                "evt_test_123",
                ReplaySource.OUTBOX,
                "ops_user_123",
                Instant.parse("2026-06-01T10:00:00Z")
        );

        assertThat(job.replayJobId().value()).isEqualTo("replay_job_123");
        assertThat(job.targetId()).isEqualTo("evt_test_123");
        assertThat(job.source()).isEqualTo(ReplaySource.OUTBOX);
        assertThat(job.requestedBy()).isEqualTo("ops_user_123");
        assertThat(job.status()).isEqualTo(ReplayJobStatus.REQUESTED);
        assertThat(job.failureReason()).isEmpty();
    }

    @Test
    void canMoveToRunning() {
        var job = requestedJob().markRunning();

        assertThat(job.status()).isEqualTo(ReplayJobStatus.RUNNING);
        assertThat(job.failureReason()).isEmpty();
    }

    @Test
    void canMoveToSucceeded() {
        var job = requestedJob()
                .markRunning()
                .markSucceeded();

        assertThat(job.status()).isEqualTo(ReplayJobStatus.SUCCEEDED);
        assertThat(job.failureReason()).isEmpty();
    }

    @Test
    void canMoveToFailedWithReason() {
        var job = requestedJob().markFailed("Kafka unavailable");

        assertThat(job.status()).isEqualTo(ReplayJobStatus.FAILED);
        assertThat(job.failureReason()).contains("Kafka unavailable");
    }

    @Test
    void canMoveToRejectedWithReason() {
        var job = requestedJob().markRejected("Target is not replayable");

        assertThat(job.status()).isEqualTo(ReplayJobStatus.REJECTED);
        assertThat(job.failureReason()).contains("Target is not replayable");
    }

    @Test
    void rejectsMissingTargetId() {
        assertThatThrownBy(() -> ReplayJob.requested(
                ReplayJobId.of("replay_job_123"),
                " ",
                ReplaySource.OUTBOX,
                "ops_user_123",
                Instant.parse("2026-06-01T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target id is required");
    }

    private ReplayJob requestedJob() {
        return ReplayJob.requested(
                ReplayJobId.of("replay_job_123"),
                "evt_test_123",
                ReplaySource.OUTBOX,
                "ops_user_123",
                Instant.parse("2026-06-01T10:00:00Z")
        );
    }
}