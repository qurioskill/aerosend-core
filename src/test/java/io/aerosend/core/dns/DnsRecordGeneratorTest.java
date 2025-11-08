package io.aerosend.core.dns;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DnsRecordGeneratorTest {

    private final DnsRecordGenerator generator = new DnsRecordGenerator("aero1", "dmarc@aerosend.io");

    @Test
    void generatesSpfDkimAndDmarc() {
        List<DnsRecord> records = generator.recordsFor("getacme.com", List.of("10.0.0.1", "10.0.0.2"), "MIIBIj");

        assertThat(records).extracting(DnsRecord::host)
            .containsExactly("getacme.com", "aero1._domainkey.getacme.com", "_dmarc.getacme.com");
        assertThat(records.get(0).value()).isEqualTo("v=spf1 ip4:10.0.0.1 ip4:10.0.0.2 -all");
    }
}
