package com.aetherstream.domain.event;

/** Semantic event type strings written to the outbox and relayed to Kafka. */
public final class EventTypes {

    public static final String TURBINE_TELEMETRY_RECORDED = "TurbineTelemetryRecorded";
    public static final String GRID_LOAD_RECORDED = "GridLoadRecorded";
    public static final String ENERGY_STATE_COMPUTED = "EnergyStateComputed";
    public static final String ALERT_RAISED = "AlertRaised";

    private EventTypes() {
    }
}
