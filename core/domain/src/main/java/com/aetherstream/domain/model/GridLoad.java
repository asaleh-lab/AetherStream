package com.aetherstream.domain.model;

import java.time.Instant;

/**
 * Grid demand and supply for a region at a point in time.
 *
 * @param region    region the reading applies to
 * @param demandMW  electricity demand in megawatts
 * @param supplyMW  electricity supply in megawatts
 * @param timestamp time the reading was taken
 */
public record GridLoad(
        String region,
        double demandMW,
        double supplyMW,
        Instant timestamp) {
}
