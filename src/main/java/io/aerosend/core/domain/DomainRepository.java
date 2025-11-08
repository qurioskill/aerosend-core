package io.aerosend.core.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DomainRepository extends JpaRepository<Domain, Long> {
    List<Domain> findByCustomerId(Long customerId);
    Optional<Domain> findByName(String name);
    long countByServerBatchId(Long serverBatchId);
}
