# Changelog

- Add GET /api/v1/domains/summary, a per-domain rollup summary over complete UTC days
- Run V2 outside a transaction so it applies on a clean database
- Add retention policy for deliverability_events
- Index domains by server batch
- Bump Spring Boot to 3.3.5
