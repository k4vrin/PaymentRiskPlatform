CREATE TABLE merchant_api_keys
(
    id          BIGSERIAL PRIMARY KEY,

    key_id      VARCHAR(80)              NOT NULL,
    secret_hash VARCHAR(255)             NOT NULL,
    merchant_id VARCHAR(120)             NOT NULL,

    status      VARCHAR(40)              NOT NULL,

    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    rotated_at  TIMESTAMP WITH TIME ZONE,

    CONSTRAINT ux_merchant_api_keys_key_id UNIQUE (key_id)
);

CREATE INDEX ix_merchant_api_keys_merchant_id
    ON merchant_api_keys (merchant_id);

CREATE INDEX ix_merchant_api_keys_status
    ON merchant_api_keys (status);
