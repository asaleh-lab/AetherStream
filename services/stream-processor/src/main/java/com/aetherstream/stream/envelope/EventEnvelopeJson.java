package com.aetherstream.stream.envelope;

import com.aetherstream.domain.event.EventEnvelope;
import com.aetherstream.domain.event.EventTypes;
import com.aetherstream.domain.model.Alert;
import com.aetherstream.domain.model.EnergyState;
import com.aetherstream.domain.model.GridLoad;
import com.aetherstream.domain.model.Turbine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.UUID;

/** Parses and serializes {@link EventEnvelope} JSON for Kafka sources and sinks. */
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

    public static String serializeEnergyState(
            String region, EnergyState state, String correlationId) {
        var envelope = new EventEnvelope(
                UUID.randomUUID(),
                EventTypes.ENERGY_STATE_COMPUTED,
                state.timestamp(),
                correlationId,
                state);
        return serialize(envelope);
    }

    public static String serializeAlert(Alert alert, String correlationId) {
        var envelope = new EventEnvelope(
                UUID.fromString(alert.id()),
                EventTypes.ALERT_RAISED,
                alert.timestamp(),
                correlationId,
                alert);
        return serialize(envelope);
    }

    private static Object parsePayload(String eventType, JsonNode payloadNode) {
        try {
            return switch (eventType) {
                case EventTypes.TURBINE_TELEMETRY_RECORDED ->
                        MAPPER.treeToValue(payloadNode, Turbine.class);
                case EventTypes.GRID_LOAD_RECORDED -> MAPPER.treeToValue(payloadNode, GridLoad.class);
                default -> MAPPER.treeToValue(payloadNode, Object.class);
            };
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid payload for event type " + eventType, e);
        }
    }
}
