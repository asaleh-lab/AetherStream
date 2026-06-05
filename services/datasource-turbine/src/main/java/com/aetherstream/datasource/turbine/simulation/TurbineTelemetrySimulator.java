package com.aetherstream.datasource.turbine.simulation;

import com.aetherstream.datasource.turbine.client.WriteSideIngestClient;
import com.aetherstream.datasource.turbine.client.WriteSideIngestClient.TurbinePayload;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aetherstream.simulation.enabled", havingValue = "true", matchIfMissing = true)
public class TurbineTelemetrySimulator {

    private static final Logger log = LoggerFactory.getLogger(TurbineTelemetrySimulator.class);
    private static final List<String> TURBINE_IDS = List.of("T-001", "T-002", "T-003");

    private final WriteSideIngestClient ingestClient;

    public TurbineTelemetrySimulator(WriteSideIngestClient ingestClient) {
        this.ingestClient = ingestClient;
    }

    @Scheduled(fixedDelayString = "${aetherstream.simulation.interval-ms:5000}")
    public void emitTelemetry() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String turbineId = TURBINE_IDS.get(random.nextInt(TURBINE_IDS.size()));
        var payload = new TurbinePayload(
                turbineId,
                8 + random.nextDouble(0, 6),
                800 + random.nextDouble(0, 1200),
                random.nextDouble(0, 1.5));
        ingestClient.postTurbine(payload);
        log.info("Forwarded simulated turbine telemetry for {} to write-side", turbineId);
    }
}
