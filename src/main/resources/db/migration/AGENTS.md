# migration

## Overview
Flyway SQL migrations that define the PostgreSQL and TimescaleDB schema. `application.yml` loads them from `classpath:db/migration`.

## Interface
- `V1__domains_mailboxes.sql`: tables `server_batches`, `ip_pools`, `ip_addresses`, `domains`, `mailboxes`.
- `V2__deliverability_hypertable.sql`: hypertable `deliverability_events` (1-day chunks on `time`), an index on `(domain_id, time DESC)`, and the continuous aggregate `domain_daily_stats`.
- `V2__deliverability_hypertable.sql.conf`: Flyway per-migration config for V2, set to `executeInTransaction=false`.
- `V3__outbox.sql`: table `outbox_events` (`id`, `aggregatetype`, `aggregateid`, `event_type`, `payload`, `created_at`).

## Invariants
- Unique: `ip_pools.name`, `ip_addresses.address`, `domains.name` (global, not per customer), and `mailboxes (domain_id, local_part)`.
- Foreign keys: `ip_addresses.pool_id`, `ip_addresses.server_batch_id` (nullable), `domains.server_batch_id`, `mailboxes.domain_id`.
- `deliverability_events` has no primary key and no foreign keys.
- `outbox_events.id` has no default, so the inserter must supply it.
- These value lists appear in SQL comments only, with no `CHECK` constraint: `domains.burn_status`, `mailboxes.sequencer`, `deliverability_events.kind`, `deliverability_events.bounce_type`.
- `README.md` states that migrations are append-only: add a new version rather than editing an existing file.
- V2 must run outside a transaction, because TimescaleDB refuses to create `domain_daily_stats` inside one. Removing `V2__deliverability_hypertable.sql.conf` stops V2 applying on a clean database. Without a transaction, a V2 failure partway leaves its earlier statements in place.
- The schema must keep matching the JPA entities, because `spring.jpa.hibernate.ddl-auto` is `validate`.

## Side Effects
- `V2` runs `CREATE EXTENSION IF NOT EXISTS timescaledb`.
- `V2` registers `add_retention_policy('deliverability_events', INTERVAL '180 days')`, which drops data older than 180 days.
- No migration defines a refresh policy for `domain_daily_stats`.

## Verification
- Run from `repos/core`: `mvn test`
