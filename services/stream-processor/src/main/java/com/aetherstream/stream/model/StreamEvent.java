package com.aetherstream.stream.model;

import com.aetherstream.domain.event.EventEnvelope;
import com.aetherstream.domain.model.GridLoad;
import com.aetherstream.domain.model.Turbine;
import com.aetherstream.domain.model.WeatherReading;
import java.time.Instant;

/**
 * Normalized ingest event keyed by region for aggregation and anomaly processing.
 */
public record StreamEvent(
        StreamEventKind kind,
        String region,
        String sourceId,
        Instant eventTime,
        String correlationId,
        Turbine turbine,
        WeatherReading weather,
        GridLoad grid) {

    public static StreamEvent turbine(EventEnvelope envelope, Turbine turbine, String region) {
        return new StreamEvent(
                StreamEventKind.TURBINE,
                region,
                turbine.turbineId(),
                envelope.occurredAt(),
                envelope.correlationId(),
                turbine,
                null,
                null);
    }

    public static StreamEvent weather(EventEnvelope envelope, WeatherReading reading) {
        return new StreamEvent(
                StreamEventKind.WEATHER,
                reading.region(),
                reading.region(),
                envelope.occurredAt(),
                envelope.correlationId(),
                null,
                reading,
                null);
    }

    public static StreamEvent grid(EventEnvelope envelope, GridLoad gridLoad) {
        return new StreamEvent(
                StreamEventKind.GRID,
                gridLoad.region(),
                gridLoad.region(),
                envelope.occurredAt(),
                envelope.correlationId(),
                null,
                null,
                gridLoad);
    }
}
