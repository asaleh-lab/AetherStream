package com.aetherstream.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope published to {@link Topics#DEAD_LETTER_EVENTS} when the outbox relay exhausts retries.
 */
public record DeadLetterEnvelope(
        UUID originalEventId,
        String originalTopic,
        String aggregateType,
        String aggregateId,
        String eventType,
        String originalPayload,
        String errorMessage,
        Instant failedAt) {
}
