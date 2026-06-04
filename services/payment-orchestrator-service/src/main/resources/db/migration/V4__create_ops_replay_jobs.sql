CREATE TABLE ops_replay_jobs
(
    replay_job_id  VARCHAR(100) PRIMARY KEY,
    target_id      VARCHAR(100)             NOT NULL,
    source         VARCHAR(32)              NOT NULL,
    requested_by   VARCHAR(120)             NOT NULL,
    requested_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    status         VARCHAR(32)              NOT NULL,
    reason         VARCHAR(500),
    failure_reason TEXT,
    correlation_id VARCHAR(120)             NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_ops_replay_jobs_source CHECK (
        source IN ('OUTBOX', 'DEAD_LETTER')
        ),
    CONSTRAINT chk_ops_replay_jobs_status CHECK (
        status IN ('REQUESTED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'REJECTED')
        )
);

CREATE UNIQUE INDEX uq_ops_replay_jobs_active_target
    ON ops_replay_jobs (source, target_id) WHERE status IN ('REQUESTED', 'RUNNING');

CREATE INDEX idx_ops_replay_jobs_target
    ON ops_replay_jobs (source, target_id);

CREATE INDEX idx_ops_replay_jobs_status_requested_at
    ON ops_replay_jobs (status, requested_at);
