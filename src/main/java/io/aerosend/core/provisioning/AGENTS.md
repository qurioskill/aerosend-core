# provisioning

## Overview
Transactional service that creates domains and mailboxes and records an outbox event for each. `provisionDomain` is exposed through `DomainController`; nothing under `src/main/java` calls `createMailboxes` yet.

## Interface
- `DomainProvisioningService` (`@Service`), constructed with `DomainRepository`, `MailboxRepository`, `OutboxWriter`, and the required property `aerosend.provisioning.domains-per-batch`.
- `Domain provisionDomain(Long customerId, Long serverBatchId, String name)`.
- `List<Mailbox> createMailboxes(Long domainId, List<String> localParts, String displayName)`.
- `DomainProvisioningService.BatchFullException` (`RuntimeException`).

## Invariants
- `provisionDomain` throws `BatchFullException` before saving when `countByServerBatchId(serverBatchId) >= domainsPerBatch`. This is a count followed by an insert, with no lock in this class.
- Both public methods are `@Transactional`, so entity rows and outbox rows commit or roll back together.
- Every entity created here gets an outbox event. New provisioning paths must keep this, and must call `OutboxWriter` inside a transaction.
- `createMailboxes` saves all mailboxes before writing any `mailbox.created` event.

## Side Effects
- `provisionDomain`: reads the batch count, inserts into `domains`, and writes `domain.provisioned` (aggregate `domain`; payload `domainId`, `customerId`, `name`).
- `createMailboxes`: inserts into `mailboxes` and writes one `mailbox.created` per mailbox (aggregate `mailbox`; payload `mailboxId`, `domainId`, `localPart`).

## Verification
- Run from `repos/core`: `mvn test`
