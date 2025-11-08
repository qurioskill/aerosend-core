package io.aerosend.core.provisioning;

import io.aerosend.core.domain.Domain;
import io.aerosend.core.domain.DomainRepository;
import io.aerosend.core.domain.Mailbox;
import io.aerosend.core.domain.MailboxRepository;
import io.aerosend.core.outbox.OutboxWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class DomainProvisioningService {

    private final DomainRepository domains;
    private final MailboxRepository mailboxes;
    private final OutboxWriter outbox;
    private final int domainsPerBatch;

    public DomainProvisioningService(DomainRepository domains, MailboxRepository mailboxes, OutboxWriter outbox,
                                     @Value("${aerosend.provisioning.domains-per-batch}") int domainsPerBatch) {
        this.domains = domains;
        this.mailboxes = mailboxes;
        this.outbox = outbox;
        this.domainsPerBatch = domainsPerBatch;
    }

    @Transactional
    public Domain provisionDomain(Long customerId, Long serverBatchId, String name) {
        if (domains.countByServerBatchId(serverBatchId) >= domainsPerBatch) {
            throw new BatchFullException(serverBatchId, domainsPerBatch);
        }
        Domain domain = domains.save(new Domain(customerId, serverBatchId, name));
        outbox.write("domain", domain.getId(), "domain.provisioned",
            Map.of("domainId", domain.getId(), "customerId", customerId, "name", domain.getName()));
        return domain;
    }

    @Transactional
    public List<Mailbox> createMailboxes(Long domainId, List<String> localParts, String displayName) {
        List<Mailbox> created = localParts.stream()
            .map(lp -> mailboxes.save(new Mailbox(domainId, lp, displayName)))
            .toList();
        created.forEach(m -> outbox.write("mailbox", m.getId(), "mailbox.created",
            Map.of("mailboxId", m.getId(), "domainId", domainId, "localPart", m.getLocalPart())));
        return created;
    }

    public static class BatchFullException extends RuntimeException {
        public BatchFullException(Long batchId, int max) {
            super("Server batch " + batchId + " already has " + max + " domains");
        }
    }
}
