package io.aerosend.core.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mailboxes")
public class Mailbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long domainId;
    private String localPart;
    private String displayName;
    private String sequencer;
    private int dailyLimit = 30;
    private Instant createdAt = Instant.now();

    protected Mailbox() {}

    public Mailbox(Long domainId, String localPart, String displayName) {
        this.domainId = domainId;
        this.localPart = localPart;
        this.displayName = displayName;
    }

    public Long getId() { return id; }
    public Long getDomainId() { return domainId; }
    public String getLocalPart() { return localPart; }
    public String getDisplayName() { return displayName; }
    public String getSequencer() { return sequencer; }
    public int getDailyLimit() { return dailyLimit; }

    public void connectSequencer(String sequencer) { this.sequencer = sequencer; }
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }
}
