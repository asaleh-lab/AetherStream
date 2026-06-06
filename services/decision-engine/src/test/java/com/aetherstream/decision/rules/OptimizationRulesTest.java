package com.aetherstream.decision.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.aetherstream.domain.model.EnergyState;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OptimizationRulesTest {

    private static final Instant NOW = Instant.parse("2026-06-05T12:00:00Z");

    @Test
    void emitsLowEfficiencySuggestionWhenBelowTarget() {
        var state = new EnergyState("north-sea", 2000, 4.0, 0.5, NOW);

        var suggestion = OptimizationRules.evaluate(state, 0.85);

        assertThat(suggestion).isPresent();
        assertThat(suggestion.get()).contains("north-sea");
        assertThat(suggestion.get()).contains("50.0%");
    }

    @Test
    void emitsSurplusSuggestionWhenWindExceedsDemand() {
        var state = new EnergyState("baltic", 5500, 4.0, 0.95, NOW);

        var suggestion = OptimizationRules.evaluate(state, 0.85);

        assertThat(suggestion).isPresent();
        assertThat(suggestion.get()).contains("Surplus");
        assertThat(suggestion.get()).contains("baltic");
    }

    @Test
    void returnsEmptyWhenEfficiencyIsHealthyAndBalanced() {
        var state = new EnergyState("north-sea", 3800, 4.0, 0.95, NOW);

        assertThat(OptimizationRules.evaluate(state, 0.85)).isEmpty();
    }
}
