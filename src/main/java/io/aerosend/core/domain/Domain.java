package io.aerosend.core.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "domains")
public class Domain {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long customerId;
    private Long serverBatchId;
    private String name;
    @Enumerated(EnumType.STRING)
    private BurnStatus burnStatus = BurnStatus.HEALTHY;
    private boolean dnsVerified;
    private Instant createdAt = Instant.now();

    protected Domain() {}

    public Domain(Long customerId, Long serverBatchId, String name) {
        this.customerId = customerId;
        this.serverBatchId = serverBatchId;
        this.name = name.toLowerCase();
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public Long getServerBatchId() { return serverBatchId; }
    public String getName() { return name; }
    public BurnStatus getBurnStatus() { return burnStatus; }
    public boolean isDnsVerified() { return dnsVerified; }
    public Instant getCreatedAt() { return createdAt; }

    public void markDnsVerified() { this.dnsVerified = true; }
    public void changeBurnStatus(BurnStatus status) { this.burnStatus = status; }
}
