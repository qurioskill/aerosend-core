# resources

## Overview
Classpath resources. `application.yml` sets the datasource, Flyway location, `server.port` 8081, and the `aerosend.provisioning` and `aerosend.dns` properties. `aerosend.provisioning.default-mailboxes-per-domain` is set here but no Java code reads it.

## Subdirectories & Boundaries
- db: [./db/AGENTS.md](./db/AGENTS.md) - Database assets, including the Flyway migrations.

## Verification
- Run from `repos/core`: `mvn test`
