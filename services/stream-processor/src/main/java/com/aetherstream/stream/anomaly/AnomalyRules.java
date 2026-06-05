package com.aetherstream.stream.anomaly;

import com.aetherstream.domain.model.Alert;
import com.aetherstream.domain.model.AlertType;
import com.aetherstream.domain.model.GridLoad;
import com.aetherstream.domain.model.Severity;
import com.aetherstream.domain.model.Turbine;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Rule-based anomaly detection for turbine telemetry and grid load. */
public final class AnomalyRules {

    private static final double FAILURE_RPM_THRESHOLD = 5.0;
    private static final double FAILURE_VIBRATION_THRESHOLD = 0.8;
    private static final double GRID_OVERLOAD_MARGIN = 1.05;

    private AnomalyRules() {}

    public static List<Alert> evaluateTurbine(Turbine turbine, double vibrationThreshold, Instant eventTime) {
        List<Alert> alerts = new ArrayList<>();

        if (turbine.vibrationLevel() >= vibrationThreshold) {
            alerts.add(new Alert(
                    UUID.randomUUID().toString(),
                    AlertType.VIBRATION_SPIKE,
                    Severity.WARNING,
                    turbine.turbineId(),
                    "Vibration level %.2f exceeded threshold %.2f"
                            .formatted(turbine.vibrationLevel(), vibrationThreshold),
                    eventTime));
        }

        if (turbine.rpm() < FAILURE_RPM_THRESHOLD && turbine.vibrationLevel() >= FAILURE_VIBRATION_THRESHOLD) {
            alerts.add(new Alert(
                    UUID.randomUUID().toString(),
                    AlertType.TURBINE_FAILURE_PATTERN,
                    Severity.CRITICAL,
                    turbine.turbineId(),
                    "Low RPM (%.1f) with elevated vibration (%.2f) indicates failure risk"
                            .formatted(turbine.rpm(), turbine.vibrationLevel()),
                    eventTime));
        }

        return alerts;
    }

    public static List<Alert> evaluateGrid(GridLoad gridLoad, Instant eventTime) {
        List<Alert> alerts = new ArrayList<>();

        if (gridLoad.demandMW() > gridLoad.supplyMW() * GRID_OVERLOAD_MARGIN) {
            alerts.add(new Alert(
                    UUID.randomUUID().toString(),
                    AlertType.GRID_OVERLOAD_RISK,
                    Severity.CRITICAL,
                    gridLoad.region(),
                    "Grid demand %.1f MW exceeds supply %.1f MW"
                            .formatted(gridLoad.demandMW(), gridLoad.supplyMW()),
                    eventTime));
        }

        return alerts;
    }
}
