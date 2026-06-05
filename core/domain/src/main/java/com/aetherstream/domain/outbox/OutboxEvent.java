package com.aetherstream.domain.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * A domain representation of a row in the transactional outbox.
 *
 * <p>The {@code id} doubles as the event identifier used by downstream consumers for
 * idempotent (at-most-once-effect) processing under at-least-once delivery.
 *
 * @param id            unique event identifier (also the downstream dedupe key)
 * @param aggregateType type of aggregate that produced the event (e.g. {@code Turbine})
 * @param aggregateId   identifier of the aggregate instance
 * @param eventType     semantic event type (e.g. {@code TurbineTelemetryRecorded})
 * @param payload       serialized event body (JSON)
 * @param status        current lifecycle status
 * @param createdAt     time the row was written in the business transaction
 * @param processedAt   time the row reached a terminal status, or {@code null} while pending
 */
public record OutboxEvent(
        UUID id,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        OutboxStatus status,
        Instant createdAt,
        Instant processedAt) {

    /** Creates a new PENDING outbox event with a fresh id and creation timestamp. */
    public static OutboxEvent pending(
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload) {
        return new OutboxEvent(
                UUID.randomUUID(),
                aggregateType,
                aggregateId,
                eventType,
                payload,
                OutboxStatus.PENDING,
                Instant.now(),
                null);
    }
}
