package com.aetherstream.domain.model;

import java.time.Instant;

/**
 * Environmental conditions for a region at a point in time.
 *
 * @param region       region the reading applies to
 * @param windSpeedMs  wind speed in metres per second
 * @param temperatureC ambient temperature in degrees Celsius
 * @param timestamp    time the reading was taken
 */
public record WeatherReading(
        String region,
        double windSpeedMs,
        double temperatureC,
        Instant timestamp) {
}
