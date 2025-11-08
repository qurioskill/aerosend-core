CREATE TABLE server_batches (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT      NOT NULL,
    region          TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ip_pools (
    id              BIGSERIAL PRIMARY KEY,
    name            TEXT        NOT NULL UNIQUE,
    warmup_strategy TEXT        NOT NULL DEFAULT 'standard',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ip_addresses (
    id              BIGSERIAL PRIMARY KEY,
    pool_id         BIGINT      NOT NULL REFERENCES ip_pools(id),
    server_batch_id BIGINT      REFERENCES server_batches(id),
    address         INET        NOT NULL UNIQUE,
    health_score    NUMERIC(5,2) NOT NULL DEFAULT 100,
    active          BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE domains (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT      NOT NULL,
    server_batch_id BIGINT      NOT NULL REFERENCES server_batches(id),
    name            TEXT        NOT NULL UNIQUE,
    burn_status     TEXT        NOT NULL DEFAULT 'HEALTHY', -- HEALTHY | AT_RISK | BURNED
    dns_verified    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE mailboxes (
    id              BIGSERIAL PRIMARY KEY,
    domain_id       BIGINT      NOT NULL REFERENCES domains(id),
    local_part      TEXT        NOT NULL,
    display_name    TEXT        NOT NULL,
    sequencer       TEXT,       -- SMARTLEAD | INSTANTLY | EMAILBISON | LEMLIST | SALESHANDY | WOODPECKER
    daily_limit     INT         NOT NULL DEFAULT 30,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (domain_id, local_part)
);
