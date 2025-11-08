package io.aerosend.core.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Writes domain events to outbox_events. MUST be called inside the business transaction;
 * Debezium (CDC) picks the row up and publishes it to RabbitMQ exchange "aerosend.events".
 */
@Component
public class OutboxWriter {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public OutboxWriter(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void write(String aggregateType, Object aggregateId, String eventType, Object payload) {
        try {
            jdbc.update("""
                INSERT INTO outbox_events (id, aggregatetype, aggregateid, event_type, payload)
                VALUES (?, ?, ?, ?, ?::jsonb)
                """, UUID.randomUUID(), aggregateType, String.valueOf(aggregateId), eventType,
                mapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write outbox event " + eventType, e);
        }
    }
}
