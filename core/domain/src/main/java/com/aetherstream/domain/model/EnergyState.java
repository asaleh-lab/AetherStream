package com.aetherstream.domain.model;

import java.time.Instant;

/**
 * Aggregated energy state for a region, produced by the stream-processing layer.
 *
 * @param region          region the state applies to
 * @param totalWindPower  aggregated wind power output in kW
 * @param gridDemand      grid demand in MW at aggregation time
 * @param efficiencyScore derived efficiency score in the range [0, 1]
 * @param timestamp       time the state was computed
 */
public record EnergyState(
        String region,
        double totalWindPower,
        double gridDemand,
        double efficiencyScore,
        Instant timestamp) {
}
