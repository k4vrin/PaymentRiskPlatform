CREATE TABLE settlement_projections
(
    id              BIGSERIAL PRIMARY KEY,

    payment_id      VARCHAR(120)             NOT NULL,
    merchant_id     VARCHAR(120),
    customer_id     VARCHAR(120),

    amount_minor    BIGINT,
    currency        VARCHAR(3),

    status          VARCHAR(40)              NOT NULL,
    business_date   DATE                     NOT NULL,

    last_event_id   VARCHAR(120)             NOT NULL,
    last_event_type VARCHAR(120)             NOT NULL,
    correlation_id  VARCHAR(120),

    authorized_at   TIMESTAMP WITH TIME ZONE,
    declined_at     TIMESTAMP WITH TIME ZONE,
    reversed_at     TIMESTAMP WITH TIME ZONE,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX ux_settlement_projections_payment_id
    ON settlement_projections (payment_id);

CREATE INDEX ix_settlement_projections_merchant_status_business_date
    ON settlement_projections (merchant_id, status, business_date);

CREATE INDEX ix_settlement_projections_status
    ON settlement_projections (status);

CREATE INDEX ix_settlement_projections_business_date
    ON settlement_projections (business_date);

CREATE INDEX ix_settlement_projections_updated_at
    ON settlement_projections (updated_at);
