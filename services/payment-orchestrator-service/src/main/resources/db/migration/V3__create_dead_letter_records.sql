CREATE TABLE dead_letter_records
(
    dead_letter_id   VARCHAR(100) PRIMARY KEY,
    source_system    VARCHAR(40)              NOT NULL,
    destination_name VARCHAR(200)             NOT NULL,
    kafka_partition  INTEGER,
    kafka_offset     BIGINT,
    delivery_tag     VARCHAR(120),
    event_id         VARCHAR(100),
    message_id       VARCHAR(120),
    status           VARCHAR(32)              NOT NULL,
    failure_reason   VARCHAR(1000)            NOT NULL,
    failed_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    replay_eligible  BOOLEAN                  NOT NULL DEFAULT FALSE,
    correlation_id   VARCHAR(120),
    payload_preview  TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_dead_letter_records_source CHECK (
        source_system IN ('KAFKA', 'RABBITMQ')
        ),
    CONSTRAINT chk_dead_letter_records_status CHECK (
        status IN ('RECORDED', 'REPLAY_REQUESTED', 'REPLAYED', 'DISCARDED')
        )
);

CREATE INDEX idx_dead_letter_records_source_status_failed_at
    ON dead_letter_records (source_system, status, failed_at);

CREATE INDEX idx_dead_letter_records_event_id
    ON dead_letter_records (event_id);

CREATE INDEX idx_dead_letter_records_message_id
    ON dead_letter_records (message_id);
