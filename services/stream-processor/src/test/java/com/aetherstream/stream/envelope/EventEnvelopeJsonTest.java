package com.aetherstream.stream.envelope;

import static org.assertj.core.api.Assertions.assertThat;

import com.aetherstream.domain.event.EventEnvelope;
import com.aetherstream.domain.event.EventTypes;
import com.aetherstream.domain.model.Turbine;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventEnvelopeJsonTest {

    @Test
    void roundTripsTurbineEnvelope() {
        UUID eventId = UUID.randomUUID();
        String correlationId = "corr-json-001";
        Instant occurredAt = Instant.parse("2026-06-05T12:00:00Z");
        var turbine = new Turbine("T-JSON-001", 11.0, 1300.0, 0.5, occurredAt);
        String json =
                """
                {"eventId":"%s","eventType":"%s","occurredAt":"2026-06-05T12:00:00Z",\
                "correlationId":"%s","payload":{"turbineId":"T-JSON-001","rpm":11.0,\
                "powerOutput":1300.0,"vibrationLevel":0.5,"timestamp":"2026-06-05T12:00:00Z"}}
                """
                        .formatted(eventId, EventTypes.TURBINE_TELEMETRY_RECORDED, correlationId);

        EventEnvelope parsed = EventEnvelopeJson.parse(json);

        assertThat(parsed.eventId()).isEqualTo(eventId);
        assertThat(parsed.correlationId()).isEqualTo(correlationId);
        assertThat(parsed.payload()).isInstanceOf(Turbine.class);
        assertThat(((Turbine) parsed.payload()).turbineId()).isEqualTo("T-JSON-001");
    }
}
