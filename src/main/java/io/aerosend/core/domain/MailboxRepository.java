package io.aerosend.core.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MailboxRepository extends JpaRepository<Mailbox, Long> {
    List<Mailbox> findByDomainId(Long domainId);
}
