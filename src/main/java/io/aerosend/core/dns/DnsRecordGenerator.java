package io.aerosend.core.dns;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/** Generates the SPF, DKIM and DMARC records a customer domain needs before mailboxes can send. */
@Component
public class DnsRecordGenerator {

    private final String dkimSelector;
    private final String dmarcRua;

    public DnsRecordGenerator(@Value("${aerosend.dns.dkim-selector}") String dkimSelector,
                              @Value("${aerosend.dns.dmarc-rua}") String dmarcRua) {
        this.dkimSelector = dkimSelector;
        this.dmarcRua = dmarcRua;
    }

    public List<DnsRecord> recordsFor(String domain, List<String> sendingIps, String dkimPublicKey) {
        String spf = "v=spf1 " + String.join(" ", sendingIps.stream().map(ip -> "ip4:" + ip).toList()) + " -all";
        return List.of(
            new DnsRecord("TXT", domain, spf),
            new DnsRecord("TXT", dkimSelector + "._domainkey." + domain, "v=DKIM1; k=rsa; p=" + dkimPublicKey),
            new DnsRecord("TXT", "_dmarc." + domain, "v=DMARC1; p=none; rua=mailto:" + dmarcRua)
        );
    }
}
