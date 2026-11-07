# api

## Overview
Internal REST layer for domains. A single controller maps HTTP requests onto the `domain` repositories, the provisioning service, and the deliverability queries. It holds no business logic of its own.

## Interface
- `DomainController` (`@RestController`, base path `/api/v1/domains`), constructed with `DomainRepository`, `DomainProvisioningService`, `DeliverabilityQueries`.
- `GET /api/v1/domains?customerId={Long}` returns `List<Domain>` (`list`).
- `GET /api/v1/domains/summary?customerId={Long}` returns `List<DeliverabilityQueries.DomainSummary>` (`summary`). Web is its only consumer.
- `POST /api/v1/domains` takes a JSON `ProvisionRequest` and returns `Domain` (`provision`).
- `GET /api/v1/domains/{id}/stats?days={int}` returns `List<DeliverabilityQueries.DailyStats>` (`stats`).
- `DomainController.ProvisionRequest`: record of `Long customerId`, `Long serverBatchId`, `String name`.

## Invariants
- `customerId` is a required request parameter on `list` and `summary`.
- `summary` filters by `customerId` but does not check who is asking, so any caller that can reach core can read any customer's figures.
- `summary` returns counts and dates only. Rates, sorting and any rank stay in web, and its field names are a contract with web's `CoreApiClient`.
- `days` defaults to 14 on `stats`; the controller applies no range check.
- `ProvisionRequest` declares `@NotNull` and `@NotBlank`, but the `provision` parameter has no `@Valid` or `@Validated`, so this controller does not trigger those constraints.
- This controller is the only one in core. `repos/admin`, `aeroctl` and `ops-dashboard` call `/api/v1/ips/*`, `/api/v1/ip-pools*` and `/api/v1/domains/{id}/replace`, which do not exist here as of this writing.
- Batch capacity and name lowercasing are enforced downstream, in `DomainProvisioningService.provisionDomain` and the `Domain` constructor, not here.

## Side Effects
- `provision` is the only mutating path. Through `DomainProvisioningService.provisionDomain` it inserts a `domains` row and an `outbox_events` row (`domain.provisioned`) in one transaction.
- `list`, `summary` and `stats` are read-only.

## Verification
- Run from `repos/core`: `mvn test`
