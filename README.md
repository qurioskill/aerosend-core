# aerosend-core

The system of record for Aerosend's sending infrastructure: **domains, mailboxes (inboxes), dedicated IPs/servers, and deliverability telemetry**.

| | |
|---|---|
| Category | core |
| Change frequency | **low** (changes are infrequent and go through extra review) |
| Stack | Java 21, Spring Boot 3, PostgreSQL 16 + TimescaleDB, Flyway, RabbitMQ, Debezium (CDC) |
| Owners | Platform team |

## What lives here

- `domain/` – JPA entities: `Domain`, `Mailbox`, `IpPool`. A customer provisions domains in **batches of 10**; each batch gets its own server + IP set.
- `provisioning/` – domain + mailbox provisioning, including DNS (SPF/DKIM/DMARC) record generation.
- `deliverability/` – writes and queries the `deliverability_events` Timescale hypertable (bounces, replies, placement tests, warmup scores).
- `outbox/` – transactional outbox. Every state change writes an `outbox_events` row in the same transaction. **Debezium** tails that table (CDC) and publishes to RabbitMQ exchange `aerosend.events`. Downstream repos (`aerosend-ancillary`, `aerosend-admin`) consume from there.
- `api/` – internal REST API (`/api/v1/...`) consumed by `aerosend-web` and `aerosend-admin`.

## Contracts other repos depend on

- REST API under `/api/v1` (see `api/`)
- Event types on `aerosend.events`: `domain.provisioned`, `mailbox.created`, `domain.burn_status_changed`, `ip.rotated`
- Table + hypertable schema (Flyway migrations are append-only)

Changing any of the above is a cross-repo change. Coordinate with ancillary/admin/web owners.

## Run locally

```bash
docker compose up -d          # timescaledb, rabbitmq, debezium-server
./mvnw spring-boot:run        # or: mvn spring-boot:run
```
