package com.aetherstream.domain.event;

/** Canonical Kafka topic names shared across the platform. */
public final class Topics {

    public static final String TURBINE_EVENTS = "turbine-events";
    public static final String GRID_EVENTS = "grid-events";
    public static final String ENERGY_STATE_EVENTS = "energy-state-events";
    public static final String ALERTS = "alerts";
    public static final String DEAD_LETTER_EVENTS = "dead-letter-events";
    public static final String OUTBOX_EVENTS = "outbox-events";

    private Topics() {
    }
}
