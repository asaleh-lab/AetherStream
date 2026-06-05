package com.aetherstream.domain.event;

/** Semantic event type strings written to the outbox and relayed to Kafka. */
public final class EventTypes {

    public static final String TURBINE_TELEMETRY_RECORDED = "TurbineTelemetryRecorded";
    public static final String WEATHER_READING_RECORDED = "WeatherReadingRecorded";
    public static final String GRID_LOAD_RECORDED = "GridLoadRecorded";

    private EventTypes() {
    }
}
