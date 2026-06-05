package com.aetherstream.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Versioned JSON envelope stored in the outbox {@code payload} column and published to Kafka.
 *
 * @param eventId        unique event identifier (matches the outbox row id)
 * @param eventType      semantic event type
 * @param occurredAt     time the business event occurred
 * @param correlationId  trace id propagated across the pipeline
 * @param payload        domain-specific event body
 */
public record EventEnvelope(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String correlationId,
        Object payload) {
}
