package com.aetherstream.datasource.grid;

import com.aetherstream.datasource.client.WriteSideIngestClient;
import com.aetherstream.datasource.client.WriteSideIngestClient.GridPayload;
import java.util.List;
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
    private static final List<String> REGIONS = List.of("north-sea", "baltic");

    private final WriteSideIngestClient ingestClient;

    public GridLoadSimulator(WriteSideIngestClient ingestClient) {
        this.ingestClient = ingestClient;
    }

    @Scheduled(fixedDelayString = "${aetherstream.grid.interval-ms:15000}")
    public void emitGridLoad() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String region = REGIONS.get(random.nextInt(REGIONS.size()));
        double demand = 400 + random.nextDouble(0, 200);
        double supply = demand - 20 + random.nextDouble(0, 80);
        ingestClient.postGrid(new GridPayload(region, demand, supply));
        log.info("Forwarded simulated grid load for {} to write-side", region);
    }
}
