-- Transactional outbox. Debezium tails this table and routes rows to RabbitMQ by event_type.
CREATE TABLE outbox_events (
    id             UUID        PRIMARY KEY,
    aggregatetype  TEXT        NOT NULL,
    aggregateid    TEXT        NOT NULL,
    event_type     TEXT        NOT NULL,
    payload        JSONB       NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
