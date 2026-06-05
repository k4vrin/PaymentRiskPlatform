CREATE TABLE payment_audit_events
(
    id             BIGSERIAL PRIMARY KEY,

    event_id       VARCHAR(120)             NOT NULL,
    event_type     VARCHAR(120)             NOT NULL,
    payment_id     VARCHAR(120)             NOT NULL,
    aggregate_type VARCHAR(120)             NOT NULL,
    schema_version VARCHAR(40)              NOT NULL,
    correlation_id VARCHAR(120),

    occurred_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    payload_json   TEXT                     NOT NULL,

    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX ux_payment_audit_events_event_id
    ON payment_audit_events (event_id);

CREATE INDEX ix_payment_audit_events_payment_id
    ON payment_audit_events (payment_id);

CREATE INDEX ix_payment_audit_events_occurred_at
    ON payment_audit_events (occurred_at);

CREATE INDEX ix_payment_audit_events_payment_occurred
    ON payment_audit_events (payment_id, occurred_at);