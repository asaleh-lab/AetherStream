package com.aetherstream.infrastructure.messaging;

import com.aetherstream.domain.event.EventEnvelope;
import com.aetherstream.domain.event.EventTypes;
import com.aetherstream.domain.model.Alert;
import com.aetherstream.domain.model.EnergyState;
import com.aetherstream.domain.model.Recommendation;
import com.aetherstream.domain.model.Turbine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;

/** Parses versioned {@link EventEnvelope} JSON from Kafka for read-model projections. */
public final class EventEnvelopeParser {

    private final ObjectMapper objectMapper;

    public EventEnvelopeParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EventEnvelope parse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
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

    private Object parsePayload(String eventType, JsonNode payloadNode) throws JsonProcessingException {
        return switch (eventType) {
            case EventTypes.ENERGY_STATE_COMPUTED ->
                    objectMapper.treeToValue(payloadNode, EnergyState.class);
            case EventTypes.ALERT_RAISED -> objectMapper.treeToValue(payloadNode, Alert.class);
            case EventTypes.RECOMMENDATION_ISSUED ->
                    objectMapper.treeToValue(payloadNode, Recommendation.class);
            case EventTypes.TURBINE_TELEMETRY_RECORDED ->
                    objectMapper.treeToValue(payloadNode, Turbine.class);
            default -> objectMapper.treeToValue(payloadNode, Object.class);
        };
    }
}
