package com.aetherstream.datasource.turbine;

import com.aetherstream.datasource.client.WriteSideIngestClient;
import com.aetherstream.datasource.client.WriteSideIngestClient.TurbinePayload;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** High-frequency turbine telemetry simulator (typical SCADA-style cadence). */
@Component
@ConditionalOnProperty(name = "aetherstream.turbine.simulation.enabled", havingValue = "true", matchIfMissing = true)
public class TurbineTelemetrySimulator {

    private static final Logger log = LoggerFactory.getLogger(TurbineTelemetrySimulator.class);

    // 6 north-sea + 4 baltic; staggered wind phases keep totals from moving in lockstep.
    private static final List<TurbineProfile> PROFILES = List.of(
            new TurbineProfile("T-001", 12.0, 1400, 620, 0.2, 0.14),
            new TurbineProfile("T-002", 13.5, 1680, 680, 0.9, 0.12),
            new TurbineProfile("T-003", 14.0, 1520, 640, 1.6, 0.16),
            new TurbineProfile("T-004", 15.0, 1820, 720, 2.4, 0.11),
            new TurbineProfile("T-005", 11.5, 1280, 580, 3.1, 0.18),
            new TurbineProfile("T-006", 14.5, 1950, 760, 3.9, 0.10),
            new TurbineProfile("T-007", 10.5, 920, 420, 0.6, 0.19),
            new TurbineProfile("T-008", 11.0, 1040, 460, 1.3, 0.17),
            new TurbineProfile("T-009", 10.0, 860, 390, 2.0, 0.21),
            new TurbineProfile("T-010", 11.5, 1120, 480, 2.8, 0.15));

    private final WriteSideIngestClient ingestClient;
    private final Map<String, Double> lastPowerKw = new ConcurrentHashMap<>();

    public TurbineTelemetrySimulator(WriteSideIngestClient ingestClient) {
        this.ingestClient = ingestClient;
    }

    @Scheduled(fixedDelayString = "${aetherstream.turbine.interval-ms:5000}")
    public void emitTelemetry() {
        long epochSec = System.currentTimeMillis() / 1000L;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double totalKw = 0;

        for (TurbineProfile profile : PROFILES) {
            double diurnal = 0.82 + 0.18 * Math.sin(epochSec / 95.0 + profile.windPhase());
            double gust = random.nextDouble(-0.16, 0.16);
            double targetKw = profile.basePowerKw() * diurnal * (1.0 + gust);

            double lastKw = lastPowerKw.getOrDefault(profile.turbineId(), profile.basePowerKw());
            double powerKw = smooth(lastKw, targetKw, 0.35);
            powerKw = clamp(powerKw, profile.minPowerKw(), profile.maxPowerKw());
            lastPowerKw.put(profile.turbineId(), powerKw);
            totalKw += powerKw;

            double rpm = profile.baseRpm()
                    + (powerKw / profile.basePowerKw() - 1.0) * 5.0
                    + random.nextDouble(-0.6, 0.6);
            double vibration = profile.baseVibration() + random.nextDouble(0, 0.9);

            ingestClient.postTurbine(new TurbinePayload(profile.turbineId(), rpm, powerKw, vibration));
        }

        log.info(
                "Forwarded simulated telemetry for {} turbines (fleet total {} kW)",
                PROFILES.size(),
                Math.round(totalKw));
    }

    private static double smooth(double previous, double target, double blend) {
        return previous * (1.0 - blend) + target * blend;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record TurbineProfile(
            String turbineId,
            double baseRpm,
            double basePowerKw,
            double powerSwingKw,
            double windPhase,
            double baseVibration) {

        double minPowerKw() {
            return basePowerKw - powerSwingKw;
        }

        double maxPowerKw() {
            return basePowerKw + powerSwingKw;
        }
    }
}
