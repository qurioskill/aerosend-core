# outbox

## Overview
Transactional outbox writer. It inserts event rows into `outbox_events` inside the caller's transaction. Publication happens outside the Java code: [Debezium configuration](../../../../../../../debezium/application.properties) tails the table and sinks to the RabbitMQ exchange `aerosend.events`.

## Interface
- `OutboxWriter` (`@Component`), constructed with `JdbcTemplate` and `ObjectMapper`.
- `void write(String aggregateType, Object aggregateId, String eventType, Object payload)`.

## Invariants
- `write` is `@Transactional(propagation = Propagation.MANDATORY)`: it must run inside the business transaction, and a proxied call with no active transaction is rejected.
- Each row gets a new `UUID.randomUUID()` id; callers cannot supply one.
- `aggregateId` is stored via `String.valueOf`; `payload` is serialized by `ObjectMapper` and cast to `jsonb`.
- Any exception is rethrown as `IllegalStateException("Failed to write outbox event " + eventType, e)`.

## Side Effects
- Inserts one `outbox_events` row (`id`, `aggregatetype`, `aggregateid`, `event_type`, `payload`) per call.

## Verification
- Run from `repos/core`: `mvn test`
