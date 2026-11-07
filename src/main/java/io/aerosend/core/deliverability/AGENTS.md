# deliverability

## Overview
Read-side access to daily deliverability statistics, queried with `JdbcTemplate` from the TimescaleDB continuous aggregate `domain_daily_stats`. This package contains no write path to `deliverability_events`.

## Interface
- `DeliverabilityQueries` (`@Repository`), constructed with `JdbcTemplate`.
- `List<DailyStats> dailyStats(long domainId, int days)`.
- `List<DomainSummary> domainSummaries(long customerId)`.
- `DeliverabilityQueries.DomainSummary`: record of `long domainId`, `WindowCounts last14Days`, `WindowCounts last3Days`, `Double latestPlacement`, `LocalDate latestPlacementDay`, `LocalDate lastDataDay`.
- `DeliverabilityQueries.WindowCounts`: record of `long sent`, `long bounces`, `long replies`.
- `DeliverabilityQueries.DailyStats`: record of `LocalDate day`, `long sent`, `long bounces`, `long replies`, `Double warmupScore`, `Double inboxPlacement`, with `bounceRate()` and `replyRate()`.

## Invariants
- `bounceRate()` and `replyRate()` return 0 when `sent == 0`.
- `dailyStats` filters on `domain_id = ?` and `day > now() - make_interval(days => ?)` (exclusive lower bound), ordered by ascending `day`.
- `warmupScore` and `inboxPlacement` are nullable; the row mapper reads them with `(Double) rs.getObject`.
- `domainId` and `days` are not validated here.
- `domainSummaries` returns one row per domain the customer owns, ordered by `id`, including domains with no rollup rows. A silent domain has zero counts and null dates.
- `domainSummaries` covers complete UTC days only: `last14Days` is the 14 days before today and `last3Days` the 3 days before today, and today is excluded. This differs from `dailyStats`, which counts back from the current time and includes today.
- The `domainSummaries` window bounds are built from a UTC date, not by subtracting an interval from a `timestamptz`, so the session time zone cannot shift them. Its dates are also taken in UTC.
- `latestPlacement` and `latestPlacementDay` come from the most recent day in the 14-day window with a non-null `inbox_placement`, and are null if there is none. `lastDataDay` is the most recent day with a rollup row in the window.

## Side Effects
- One parameterized `SELECT` against `domain_daily_stats` per `dailyStats` call. No writes.
- One parameterized `SELECT` joining `domains` and `domain_daily_stats` per `domainSummaries` call. No writes.

## Verification
- Run from `repos/core`: `mvn test`. `DomainSummaryQueriesTest` covers `domainSummaries` on a real TimescaleDB and needs Docker.
