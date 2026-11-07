# core

## Overview
Root package of the `aerosend-core` Spring Boot application. `CoreApplication` boots the context, and component scanning from `io.aerosend.core` picks up the child packages.

## Interface
- `CoreApplication` (`@SpringBootApplication`) with `main(String[] args)`.
- [api](./api/AGENTS.md): REST endpoints under `/api/v1/domains`.
- [deliverability](./deliverability/AGENTS.md): `DeliverabilityQueries`, `DailyStats`.
- [dns](./dns/AGENTS.md): `DnsRecord`, `DnsRecordGenerator`.
- [domain](./domain/AGENTS.md): `Domain`, `Mailbox`, `BurnStatus`, `DomainRepository`, `MailboxRepository`.
- [outbox](./outbox/AGENTS.md): `OutboxWriter`.
- [provisioning](./provisioning/AGENTS.md): `DomainProvisioningService`, `BatchFullException`.

## Invariants
- `CoreApplication` holds no state, validation, or guard logic.
- `spring.jpa.hibernate.ddl-auto` is `validate` and `spring.jpa.open-in-view` is `false` in `application.yml`.

## Side Effects
- `main` starts the application context on port 8081 (`server.port`), connects to PostgreSQL at `jdbc:postgresql://localhost:5432/aerosend`, and runs Flyway from `classpath:db/migration`.

## Verification
- Run from `repos/core`: `mvn test`
