package com.aetherstream.domain.model;

import java.time.Instant;

/**
 * An optimization recommendation produced by the decision engine.
 *
 * @param id         unique recommendation identifier
 * @param region     region the recommendation applies to
 * @param suggestion actionable suggestion (e.g. turbine adjustment, grid balancing)
 * @param timestamp  time the recommendation was produced
 */
public record Recommendation(
        String id,
        String region,
        String suggestion,
        Instant timestamp) {
}
