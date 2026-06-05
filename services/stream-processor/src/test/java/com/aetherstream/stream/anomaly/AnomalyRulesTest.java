package com.aetherstream.stream.anomaly;

import static org.assertj.core.api.Assertions.assertThat;

import com.aetherstream.domain.model.AlertType;
import com.aetherstream.domain.model.GridLoad;
import com.aetherstream.domain.model.Severity;
import com.aetherstream.domain.model.Turbine;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AnomalyRulesTest {

    private static final Instant NOW = Instant.parse("2026-06-05T12:00:00Z");

    @Test
    void evaluateTurbine_detectsVibrationSpike() {
        var turbine = new Turbine("T-001", 10, 1200, 1.2, NOW);

        var alerts = AnomalyRules.evaluateTurbine(turbine, 1.0, NOW);

        assertThat(alerts).anyMatch(a -> a.type() == AlertType.VIBRATION_SPIKE && a.severity() == Severity.WARNING);
    }

    @Test
    void evaluateTurbine_detectsFailurePattern() {
        var turbine = new Turbine("T-002", 3, 200, 0.9, NOW);

        var alerts = AnomalyRules.evaluateTurbine(turbine, 1.0, NOW);

        assertThat(alerts).anyMatch(a -> a.type() == AlertType.TURBINE_FAILURE_PATTERN);
    }

    @Test
    void evaluateGrid_detectsOverloadRisk() {
        var grid = new GridLoad("north-sea", 500, 400, NOW);

        var alerts = AnomalyRules.evaluateGrid(grid, NOW);

        assertThat(alerts).singleElement().satisfies(alert -> {
            assertThat(alert.type()).isEqualTo(AlertType.GRID_OVERLOAD_RISK);
            assertThat(alert.severity()).isEqualTo(Severity.CRITICAL);
            assertThat(alert.source()).isEqualTo("north-sea");
        });
    }
}
