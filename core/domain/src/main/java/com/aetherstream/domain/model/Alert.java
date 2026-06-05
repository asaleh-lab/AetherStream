package com.aetherstream.domain.model;

import java.time.Instant;

/**
 * A detected anomaly raised by the anomaly-detection stream.
 *
 * @param id        unique alert identifier
 * @param type      category of anomaly
 * @param severity  urgency level
 * @param source    originating entity (e.g. turbine id or region)
 * @param message   human-readable description
 * @param timestamp time the alert was raised
 */
public record Alert(
        String id,
        AlertType type,
        Severity severity,
        String source,
        String message,
        Instant timestamp) {
}
