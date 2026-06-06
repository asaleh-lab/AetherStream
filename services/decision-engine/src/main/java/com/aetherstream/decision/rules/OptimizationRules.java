package com.aetherstream.decision.rules;

import com.aetherstream.domain.model.EnergyState;
import java.util.Optional;

/** Optimization rules applied to aggregated energy state. */
public final class OptimizationRules {

    private OptimizationRules() {}

    /**
     * Evaluates the current energy state and returns an actionable suggestion when optimization
     * is warranted.
     */
    public static Optional<String> evaluate(EnergyState state, double efficiencyTarget) {
        if (state.efficiencyScore() < efficiencyTarget) {
            return Optional.of(String.format(
                    "Efficiency is %.0f%% (target %.0f%%) — increase turbine output or reduce load in %s",
                    state.efficiencyScore() * 100,
                    efficiencyTarget * 100,
                    state.region()));
        }

        double demandKw = state.gridDemand() * 1000.0;
        if (demandKw > 0 && state.totalWindPower() > demandKw * 1.05) {
            return Optional.of(String.format(
                    "Surplus wind capacity in %s — consider exporting excess power or storage dispatch",
                    state.region()));
        }

        return Optional.empty();
    }
}
