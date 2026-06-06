package com.aetherstream.datasource.grid;

import com.aetherstream.datasource.client.WriteSideIngestClient;
import com.aetherstream.datasource.client.WriteSideIngestClient.GridPayload;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Slower grid load updates (operational/market cadence, not per-second telemetry). */
@Component
@ConditionalOnProperty(name = "aetherstream.grid.simulation.enabled", havingValue = "true", matchIfMissing = true)
public class GridLoadSimulator {

    private static final Logger log = LoggerFactory.getLogger(GridLoadSimulator.class);

    // Scaled to the 10-turbine demo fleet (6 north-sea, 4 baltic).
    private static final List<RegionProfile> REGIONS = List.of(
            new RegionProfile("north-sea", 8.5, 2.5, 0.4),
            new RegionProfile("baltic", 3.2, 1.2, 1.9));

    private final WriteSideIngestClient ingestClient;
    private final Map<String, Double> lastDemandMw = new ConcurrentHashMap<>();

    public GridLoadSimulator(WriteSideIngestClient ingestClient) {
        this.ingestClient = ingestClient;
    }

    @Scheduled(fixedDelayString = "${aetherstream.grid.interval-ms:15000}")
    public void emitGridLoad() {
        long epochSec = System.currentTimeMillis() / 1000L;
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (RegionProfile profile : REGIONS) {
            double diurnal = 0.88 + 0.12 * Math.sin(epochSec / 210.0 + profile.loadPhase());
            double noise = random.nextDouble(-0.14, 0.14);
            double targetMw = profile.baseDemandMw() * diurnal * (1.0 + noise);

            double lastMw = lastDemandMw.getOrDefault(profile.region(), profile.baseDemandMw());
            double demandMw = smooth(lastMw, targetMw, 0.3);
            demandMw = clamp(demandMw, profile.minDemandMw(), profile.maxDemandMw());
            lastDemandMw.put(profile.region(), demandMw);

            // Supply trails demand with region-specific variability for imbalance alerts.
            double supplyRatio = profile.region().equals("north-sea")
                    ? 0.78 + random.nextDouble(0, 0.28)
                    : 0.68 + random.nextDouble(0, 0.32);
            double supplyMw = demandMw * supplyRatio;

            ingestClient.postGrid(new GridPayload(profile.region(), demandMw, supplyMw));
            log.info(
                    "Forwarded simulated grid load for {} ({} MW demand) to write-side",
                    profile.region(),
                    String.format("%.2f", demandMw));
        }
    }

    private static double smooth(double previous, double target, double blend) {
        return previous * (1.0 - blend) + target * blend;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record RegionProfile(String region, double baseDemandMw, double demandSwingMw, double loadPhase) {

        double minDemandMw() {
            return baseDemandMw - demandSwingMw;
        }

        double maxDemandMw() {
            return baseDemandMw + demandSwingMw;
        }
    }
}
