# domain

## Overview
JPA entities and Spring Data repositories for customer domains and mailboxes, plus the burn status enum. The tables `server_batches`, `ip_pools`, and `ip_addresses` have no entity here.

## Interface
- `Domain` (`@Table(name = "domains")`): public constructor `Domain(Long customerId, Long serverBatchId, String name)`, getters, `markDnsVerified()`, `changeBurnStatus(BurnStatus)`.
- `Mailbox` (`@Table(name = "mailboxes")`): public constructor `Mailbox(Long domainId, String localPart, String displayName)`, getters (none for `createdAt`), `connectSequencer(String)`, `setDailyLimit(int)`.
- `BurnStatus`: `HEALTHY`, `AT_RISK`, `BURNED`.
- `DomainRepository extends JpaRepository<Domain, Long>`: `findByCustomerId`, `findByName`, `countByServerBatchId`.
- `MailboxRepository extends JpaRepository<Mailbox, Long>`: `findByDomainId`.

## Invariants
- `Domain.name` is stored lowercased by the constructor; a null name throws `NullPointerException`.
- `Domain.burnStatus` defaults to `HEALTHY` and persists as a string (`EnumType.STRING`).
- `Domain.dnsVerified` starts false; `markDnsVerified` only sets it true.
- `Mailbox.dailyLimit` defaults to 30. `Mailbox.sequencer` is null until `connectSequencer` is called.
- `id` is database-generated (`GenerationType.IDENTITY`) on both entities; `createdAt` is set from `Instant.now()` at instantiation.
- Identity fields set by the constructors have no setters.
- Mapped columns must match the Flyway schema, because `spring.jpa.hibernate.ddl-auto` is `validate`. See [migrations](../../../../../resources/db/migration/AGENTS.md).

## Side Effects
- `markDnsVerified`, `changeBurnStatus`, `connectSequencer`, and `setDailyLimit` change in-memory state only, with no validation, and write no outbox event.
- Both repositories read and write the database through `JpaRepository`.

## Verification
- Run from `repos/core`: `mvn test`
