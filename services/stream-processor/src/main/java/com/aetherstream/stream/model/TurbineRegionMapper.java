package com.aetherstream.stream.model;

import java.util.Map;

/**
 * Maps turbine ids to regions for keyed aggregation (turbine events are keyed by turbineId in Kafka).
 */
public final class TurbineRegionMapper {

    private static final Map<String, String> TURBINE_REGIONS = Map.ofEntries(
            Map.entry("T-001", "north-sea"),
            Map.entry("T-002", "north-sea"),
            Map.entry("T-003", "north-sea"),
            Map.entry("T-004", "north-sea"),
            Map.entry("T-005", "north-sea"),
            Map.entry("T-006", "north-sea"),
            Map.entry("T-007", "baltic"),
            Map.entry("T-008", "baltic"),
            Map.entry("T-009", "baltic"),
            Map.entry("T-010", "baltic"));

    private TurbineRegionMapper() {}

    public static String regionFor(String turbineId) {
        return TURBINE_REGIONS.getOrDefault(turbineId, "unknown");
    }
}
