package com.aetherstream.domain.event;

/** Aggregate type strings for outbox routing and relay topic mapping. */
public final class AggregateTypes {

    public static final String TURBINE = "Turbine";
    public static final String WEATHER_READING = "WeatherReading";
    public static final String GRID_LOAD = "GridLoad";

    private AggregateTypes() {
    }
}
