package com.aetherstream.writeside.api;

import java.time.Instant;

public record WeatherIngestRequest(String region, double windSpeedMs, double temperatureC, Instant timestamp) {
}
