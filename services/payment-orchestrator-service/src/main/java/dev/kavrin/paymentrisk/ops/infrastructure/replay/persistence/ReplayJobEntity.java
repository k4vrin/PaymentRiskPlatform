package dev.kavrin.paymentrisk.ops.infrastructure.replay.persistence;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("ops_replay_jobs")
public class ReplayJobEntity implements Persistable<String> {

    @Id
    @Column("replay_job_id")
    private String replayJobId;

    @Column("target_id")
    private String targetId;

    @Column("source")
    private String source;

    @Column("requested_by")
    private String requestedBy;

    @Column("requested_at")
    private Instant requestedAt;

    @Column("status")
    private String status;

    @Column("reason")
    private String reason;

    @Column("failure_reason")
    private String failureReason;

    @Column("correlation_id")
    private String correlationId;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Transient
    @Builder.Default
    private boolean newEntity = true;

    @Override
    public String getId() {
        return replayJobId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }
}
