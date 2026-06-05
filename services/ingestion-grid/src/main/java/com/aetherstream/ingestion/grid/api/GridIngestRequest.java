package com.aetherstream.ingestion.grid.api;

import java.time.Instant;

public record GridIngestRequest(String region, double demandMW, double supplyMW, Instant timestamp) {
}
