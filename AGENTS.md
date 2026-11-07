# aerosend-core

## Overview
System of record for domains, mailboxes, and deliverability data, built with Spring Boot 3.3.5 on Java 21. It exposes a REST API on port 8081, keeps its schema in Flyway migrations on PostgreSQL with TimescaleDB, and writes `domain.provisioned` and `mailbox.created` events to `outbox_events`. Debezium tails that table and publishes to the RabbitMQ exchange `aerosend.events`. `docker-compose.yml` defines the local `postgres`, `rabbitmq`, and `debezium` services.

## Subdirectories & Boundaries
- src: [./src/AGENTS.md](./src/AGENTS.md) - Production code, resources, and tests.
- debezium: [./debezium/application.properties](./debezium/application.properties) - Configuration only, with no contract file. Routes `public.outbox_events` by `event_type` to the RabbitMQ exchange `aerosend.events`.

## Verification
- Run from `repos/core`: `mvn test`. Docker must be running: `DomainSummaryQueriesTest` starts a TimescaleDB container with Testcontainers, and it fails rather than skips without Docker.
