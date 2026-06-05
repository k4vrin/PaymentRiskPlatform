CREATE TABLE processed_kafka_messages
(
    processed_message_id VARCHAR(160) PRIMARY KEY,
    consumer_name        VARCHAR(120)             NOT NULL,
    topic                VARCHAR(249)             NOT NULL,
    partition_id         INTEGER                  NOT NULL,
    message_offset       BIGINT                   NOT NULL,
    event_id             VARCHAR(100)             NOT NULL,
    processed_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_processed_kafka_messages_consumer_event
        UNIQUE (consumer_name, event_id),
    CONSTRAINT uq_processed_kafka_messages_consumer_position
        UNIQUE (consumer_name, topic, partition_id, message_offset),
    CONSTRAINT chk_processed_kafka_messages_partition CHECK (partition_id >= 0),
    CONSTRAINT chk_processed_kafka_messages_offset CHECK (message_offset >= 0)
);

CREATE INDEX idx_processed_kafka_messages_event_id
    ON processed_kafka_messages (event_id);

CREATE INDEX idx_processed_kafka_messages_processed_at
    ON processed_kafka_messages (processed_at);
