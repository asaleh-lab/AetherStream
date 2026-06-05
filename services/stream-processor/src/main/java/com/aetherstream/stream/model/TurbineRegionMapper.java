package com.aetherstream.stream.model;

import java.util.Map;

/**
 * Maps turbine ids to regions for keyed aggregation (turbine events are keyed by turbineId in Kafka).
 */
public final class TurbineRegionMapper {

    private static final Map<String, String> TURBINE_REGIONS = Map.of(
            "T-001", "north-sea",
            "T-002", "north-sea",
            "T-003", "baltic");

    private TurbineRegionMapper() {}

    public static String regionFor(String turbineId) {
        return TURBINE_REGIONS.getOrDefault(turbineId, "unknown");
    }
}
