package com.aetherstream.domain.model;

import java.time.Instant;

/**
 * Latest telemetry snapshot for a single wind turbine.
 *
 * @param turbineId      stable identifier of the turbine
 * @param rpm            rotor speed in revolutions per minute
 * @param powerOutput    instantaneous power output in kW
 * @param vibrationLevel vibration magnitude (unitless sensor reading)
 * @param timestamp      time the reading was taken
 */
public record Turbine(
        String turbineId,
        double rpm,
        double powerOutput,
        double vibrationLevel,
        Instant timestamp) {
}
