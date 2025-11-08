CREATE EXTENSION IF NOT EXISTS timescaledb;

-- One row per signal: bounce, reply, placement test result, warmup score sample.
CREATE TABLE deliverability_events (
    time        TIMESTAMPTZ NOT NULL,
    domain_id   BIGINT      NOT NULL,
    mailbox_id  BIGINT,
    ip_id       BIGINT,
    kind        TEXT        NOT NULL, -- SENT | BOUNCE | REPLY | PLACEMENT | WARMUP_SCORE
    bounce_type TEXT,                 -- HARD | SOFT | BLOCK (only for BOUNCE)
    value       DOUBLE PRECISION
);

SELECT create_hypertable('deliverability_events', 'time', chunk_time_interval => INTERVAL '1 day');
CREATE INDEX ON deliverability_events (domain_id, time DESC);

-- Daily rollup used by burn alerts and dashboards
CREATE MATERIALIZED VIEW domain_daily_stats
WITH (timescaledb.continuous) AS
SELECT time_bucket('1 day', time) AS day,
       domain_id,
       count(*) FILTER (WHERE kind = 'SENT')   AS sent,
       count(*) FILTER (WHERE kind = 'BOUNCE') AS bounces,
       count(*) FILTER (WHERE kind = 'REPLY')  AS replies,
       avg(value) FILTER (WHERE kind = 'WARMUP_SCORE') AS warmup_score,
       avg(value) FILTER (WHERE kind = 'PLACEMENT')    AS inbox_placement
FROM deliverability_events
GROUP BY 1, 2;

SELECT add_retention_policy('deliverability_events', INTERVAL '180 days');
