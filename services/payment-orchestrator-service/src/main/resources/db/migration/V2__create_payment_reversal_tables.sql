CREATE TABLE payment_reversals
(
    payment_reversal_id VARCHAR(100) PRIMARY KEY,
    payment_id          VARCHAR(100)             NOT NULL,
    merchant_id         VARCHAR(100)             NOT NULL,
    customer_id         VARCHAR(100)             NOT NULL,
    idempotency_key     VARCHAR(128)             NOT NULL,
    reason              VARCHAR(500),
    status              VARCHAR(32)              NOT NULL,
    requested_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    reversed_at         TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_payment_reversals_payment
        FOREIGN KEY (payment_id) REFERENCES payments (payment_id),
    CONSTRAINT uq_payment_reversals_payment UNIQUE (payment_id),
    CONSTRAINT uq_payment_reversals_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT chk_payment_reversals_status CHECK (
        status IN (
                   'REQUESTED',
                   'REVERSED',
                   'FAILED'
            )
        )
);

CREATE INDEX idx_payment_reversals_payment_id
    ON payment_reversals (payment_id);

CREATE INDEX idx_payment_reversals_merchant_id
    ON payment_reversals (merchant_id);

CREATE INDEX idx_payment_reversals_customer_id
    ON payment_reversals (customer_id);
