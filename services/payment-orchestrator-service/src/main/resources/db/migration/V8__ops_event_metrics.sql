CREATE TABLE ops_event_metrics
(
    id                  BIGSERIAL PRIMARY KEY,

    metric_key          VARCHAR(160)             NOT NULL,
    metric_value        BIGINT                   NOT NULL,

    last_event_id       VARCHAR(120),
    last_event_type     VARCHAR(120),
    last_correlation_id VARCHAR(120),
    last_observed_at    TIMESTAMP WITH TIME ZONE,

    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX ux_ops_event_metrics_metric_key
    ON ops_event_metrics (metric_key);