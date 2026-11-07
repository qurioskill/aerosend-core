# db

## Overview
Groups database assets. It contains only the Flyway migrations.

## Subdirectories & Boundaries
- migration: [./migration/AGENTS.md](./migration/AGENTS.md) - Flyway schema for domains, mailboxes, IPs, deliverability telemetry, and the outbox.

## Verification
- Run from `repos/core`: `mvn test`
