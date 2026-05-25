CREATE TABLE payments
(
    payment_id                 VARCHAR(100) PRIMARY KEY,
    merchant_id                VARCHAR(100)             NOT NULL,
    customer_id                VARCHAR(100)             NOT NULL,
    amount_minor               BIGINT                   NOT NULL,
    currency                   CHAR(3)                  NOT NULL,
    payment_method_token_hash  VARCHAR(128)             NOT NULL,
    payment_method_token_last4 CHAR(4),
    device_fingerprint_hash    VARCHAR(128)             NOT NULL,
    external_reference         VARCHAR(120),
    idempotency_key            VARCHAR(128)             NOT NULL,
    status                     VARCHAR(32)              NOT NULL,
    created_at                 TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                 TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_payments_amount_positive CHECK (amount_minor > 0),
    CONSTRAINT chk_payments_status CHECK (
        status IN (
                   'RECEIVED',
                   'RISK_PENDING',
                   'RISK_APPROVED',
                   'AUTHORIZED',
                   'DECLINED',
                   'REVERSED',
                   'FAILED'
            )
        )
);

CREATE TABLE payment_authorizations
(
    payment_authorization_id VARCHAR(100) PRIMARY KEY,
    payment_id               VARCHAR(100)             NOT NULL,
    status                   VARCHAR(32)              NOT NULL,
    authorization_code       VARCHAR(40),
    failure_reason           VARCHAR(500),
    requested_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    risk_pending_at          TIMESTAMP WITH TIME ZONE,
    authorized_at            TIMESTAMP WITH TIME ZONE,
    declined_at              TIMESTAMP WITH TIME ZONE,
    failed_at                TIMESTAMP WITH TIME ZONE,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_payment_authorizations_payment
        FOREIGN KEY (payment_id) REFERENCES payments (payment_id),
    CONSTRAINT uq_payment_authorizations_payment UNIQUE (payment_id),
    CONSTRAINT chk_payment_authorizations_status CHECK (
        status IN (
                   'REQUESTED',
                   'RISK_PENDING',
                   'AUTHORIZED',
                   'DECLINED',
                   'FAILED'
            )
        )
);

CREATE TABLE payment_risk_decisions
(
    payment_risk_decision_id VARCHAR(100) PRIMARY KEY,
    payment_id               VARCHAR(100)             NOT NULL,
    decision                 VARCHAR(32)              NOT NULL,
    score                    INTEGER                  NOT NULL,
    reason_codes_json        TEXT                     NOT NULL DEFAULT '[]',
    rule_version             VARCHAR(100)             NOT NULL,
    decided_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_payment_risk_decisions_payment
        FOREIGN KEY (payment_id) REFERENCES payments (payment_id),
    CONSTRAINT uq_payment_risk_decisions_payment UNIQUE (payment_id),
    CONSTRAINT chk_payment_risk_decisions_decision CHECK (
        decision IN (
                     'APPROVED',
                     'DECLINED',
                     'REVIEW_REQUIRED'
            )
        ),
    CONSTRAINT chk_payment_risk_decisions_score CHECK (score BETWEEN 0 AND 100)
);

CREATE TABLE idempotency_records
(
    idempotency_record_id VARCHAR(100) PRIMARY KEY,
    scope                 VARCHAR(80)              NOT NULL,
    idempotency_key       VARCHAR(128)             NOT NULL,
    request_fingerprint   VARCHAR(128)             NOT NULL,
    payment_id            VARCHAR(100),
    status                VARCHAR(32)              NOT NULL,
    response_status       INTEGER,
    response_body_json    TEXT,
    expires_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_idempotency_records_payment
        FOREIGN KEY (payment_id) REFERENCES payments (payment_id),
    CONSTRAINT uq_idempotency_records_scope_key UNIQUE (scope, idempotency_key),
    CONSTRAINT chk_idempotency_records_status CHECK (
        status IN (
                   'STARTED',
                   'COMPLETED',
                   'FAILED'
            )
        ),
    CONSTRAINT chk_idempotency_records_response_status CHECK (
        response_status IS NULL OR response_status BETWEEN 100 AND 599
        )
);

CREATE TABLE outbox_events
(
    event_id       VARCHAR(100) PRIMARY KEY,
    aggregate_type VARCHAR(80)              NOT NULL,
    aggregate_id   VARCHAR(100)             NOT NULL,
    event_type     VARCHAR(120)             NOT NULL,
    schema_version VARCHAR(20)              NOT NULL,
    producer       VARCHAR(120)             NOT NULL,
    correlation_id VARCHAR(120)             NOT NULL,
    payload_json   TEXT                     NOT NULL,
    status         VARCHAR(32)              NOT NULL,
    retry_count    INTEGER                  NOT NULL DEFAULT 0,
    next_retry_at  TIMESTAMP WITH TIME ZONE,
    last_error     TEXT,
    occurred_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at   TIMESTAMP WITH TIME ZONE,
    locked_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_outbox_events_status CHECK (
        status IN (
                   'PENDING',
                   'PUBLISHING',
                   'PUBLISHED',
                   'FAILED'
            )
        ),
    CONSTRAINT chk_outbox_events_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX idx_payment_authorizations_payment_id
    ON payment_authorizations (payment_id);

CREATE INDEX idx_payment_risk_decisions_payment_id
    ON payment_risk_decisions (payment_id);

CREATE INDEX idx_payments_status_created_at
    ON payments (status, created_at);

CREATE INDEX idx_idempotency_records_payment_id
    ON idempotency_records (payment_id);

CREATE INDEX idx_payments_merchant_id
    ON payments (merchant_id);

CREATE INDEX idx_payments_customer_id
    ON payments (customer_id);

CREATE INDEX idx_payments_payment_method_token_hash
    ON payments (payment_method_token_hash);

CREATE INDEX idx_payments_device_fingerprint_hash
    ON payments (device_fingerprint_hash);

CREATE INDEX idx_outbox_events_status_next_retry_at
    ON outbox_events (status, next_retry_at);
