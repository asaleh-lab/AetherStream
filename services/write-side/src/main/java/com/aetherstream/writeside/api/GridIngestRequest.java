package com.aetherstream.writeside.api;

import java.time.Instant;

public record GridIngestRequest(String region, double demandMW, double supplyMW, Instant timestamp) {
}
