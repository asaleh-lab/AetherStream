package com.aetherstream.decision.envelope;

import com.aetherstream.domain.event.EventEnvelope;
import com.aetherstream.domain.event.EventTypes;
import com.aetherstream.domain.model.EnergyState;
import com.aetherstream.domain.model.Recommendation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.UUID;

/** Parses and serializes {@link EventEnvelope} JSON for the decision-engine Kafka topology. */
public final class EventEnvelopeJson {

    private static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private EventEnvelopeJson() {}

    public static EventEnvelope parse(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            UUID eventId = UUID.fromString(root.get("eventId").asText());
            String eventType = root.get("eventType").asText();
            Instant occurredAt = Instant.parse(root.get("occurredAt").asText());
            String correlationId = root.get("correlationId").asText();
            JsonNode payloadNode = root.get("payload");
            Object payload = parsePayload(eventType, payloadNode);
            return new EventEnvelope(eventId, eventType, occurredAt, correlationId, payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid event envelope JSON", e);
        }
    }

    public static String serialize(EventEnvelope envelope) {
        try {
            return MAPPER.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event envelope", e);
        }
    }

    public static String serializeRecommendation(Recommendation recommendation, String correlationId) {
        var envelope = new EventEnvelope(
                UUID.fromString(recommendation.id()),
                EventTypes.RECOMMENDATION_ISSUED,
                recommendation.timestamp(),
                correlationId,
                recommendation);
        return serialize(envelope);
    }

    private static Object parsePayload(String eventType, JsonNode payloadNode) {
        try {
            return switch (eventType) {
                case EventTypes.ENERGY_STATE_COMPUTED -> MAPPER.treeToValue(payloadNode, EnergyState.class);
                default -> MAPPER.treeToValue(payloadNode, Object.class);
            };
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid payload for event type " + eventType, e);
        }
    }
}
