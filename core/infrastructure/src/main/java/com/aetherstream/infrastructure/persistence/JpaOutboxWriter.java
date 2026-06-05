package com.aetherstream.infrastructure.persistence;

import com.aetherstream.application.port.out.OutboxAppender;
import com.aetherstream.application.port.out.OutboxWriter;
import com.aetherstream.domain.event.EventEnvelope;
import com.aetherstream.domain.outbox.OutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JpaOutboxWriter implements OutboxWriter {

    private final OutboxAppender outboxAppender;
    private final ObjectMapper objectMapper;

    public JpaOutboxWriter(OutboxAppender outboxAppender, ObjectMapper objectMapper) {
        this.outboxAppender = outboxAppender;
        this.objectMapper = objectMapper;
    }

    @Override
    public void writePending(
            String aggregateType,
            String aggregateId,
            String eventType,
            Object payloadBody,
            String correlationId) {
        OutboxEvent pending = OutboxEvent.pending(aggregateType, aggregateId, eventType, "{}");
        String envelopeJson = serializeEnvelope(
                pending.id(), eventType, pending.createdAt(), correlationId, payloadBody);
        outboxAppender.append(new OutboxEvent(
                pending.id(),
                pending.aggregateType(),
                pending.aggregateId(),
                pending.eventType(),
                envelopeJson,
                pending.status(),
                pending.createdAt(),
                pending.processedAt()));
    }

    private String serializeEnvelope(
            UUID eventId, String eventType, Instant occurredAt, String correlationId, Object payloadBody) {
        var envelope = new EventEnvelope(eventId, eventType, occurredAt, correlationId, payloadBody);
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event envelope", e);
        }
    }
}
