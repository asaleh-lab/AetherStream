package com.aetherstream.ingestion.turbine.api;

import java.time.Instant;

public record TurbineIngestRequest(
        String turbineId, double rpm, double powerOutput, double vibrationLevel, Instant timestamp) {
}
