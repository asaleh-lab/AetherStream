package com.aetherstream.ingestion.turbine.simulation;

import com.aetherstream.application.command.RecordTurbineTelemetryCommand;
import com.aetherstream.application.cqrs.CommandBus;
import com.aetherstream.domain.model.Turbine;
import com.aetherstream.infrastructure.correlation.CorrelationIdContext;
import java.time.Instant;
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

    private final CommandBus commandBus;
    private final CorrelationIdContext correlationIdContext;

    public TurbineTelemetrySimulator(CommandBus commandBus, CorrelationIdContext correlationIdContext) {
        this.commandBus = commandBus;
        this.correlationIdContext = correlationIdContext;
    }

    @Scheduled(fixedDelayString = "${aetherstream.simulation.interval-ms:5000}")
    public void emitTelemetry() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String turbineId = TURBINE_IDS.get(random.nextInt(TURBINE_IDS.size()));
        var turbine = new Turbine(
                turbineId,
                8 + random.nextDouble(0, 6),
                800 + random.nextDouble(0, 1200),
                random.nextDouble(0, 1.5),
                Instant.now());
        String correlationId = correlationIdContext.getOrCreate();
        commandBus.dispatch(new RecordTurbineTelemetryCommand(turbine, correlationId));
        log.info("Simulated turbine telemetry for {} (correlationId={})", turbineId, correlationId);
    }
}
