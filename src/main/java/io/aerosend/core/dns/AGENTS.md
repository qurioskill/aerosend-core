# dns

## Overview
Pure generation of the SPF, DKIM, and DMARC TXT records for a customer domain. Nothing under `src/main/java` calls `DnsRecordGenerator` yet.

## Interface
- `DnsRecord`: record of `String type`, `String host`, `String value`.
- `DnsRecordGenerator` (`@Component`), constructed from the required properties `aerosend.dns.dkim-selector` and `aerosend.dns.dmarc-rua`.
- `List<DnsRecord> recordsFor(String domain, List<String> sendingIps, String dkimPublicKey)`.

## Invariants
- `recordsFor` returns exactly three immutable `TXT` records, in the order SPF, DKIM, DMARC.
- SPF: host is `domain`; value lists each IP as `ip4:<ip>` and ends in `-all`.
- DKIM: host is `<dkimSelector>._domainkey.<domain>`; value is `v=DKIM1; k=rsa; p=<dkimPublicKey>`.
- DMARC: host is `_dmarc.<domain>`; value is `v=DMARC1; p=none; rua=mailto:<dmarcRua>`. The policy is fixed at `none`.

## Side Effects
- None.

## Verification
- Run from `repos/core`: `mvn test`
- Covered by `DnsRecordGeneratorTest`.
