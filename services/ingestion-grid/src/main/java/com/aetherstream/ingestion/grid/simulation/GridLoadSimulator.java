package com.aetherstream.ingestion.grid.simulation;

import com.aetherstream.application.command.RecordGridLoadCommand;
import com.aetherstream.application.cqrs.CommandBus;
import com.aetherstream.domain.model.GridLoad;
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
public class GridLoadSimulator {

    private static final Logger log = LoggerFactory.getLogger(GridLoadSimulator.class);
    private static final List<String> REGIONS = List.of("north-sea", "baltic");

    private final CommandBus commandBus;
    private final CorrelationIdContext correlationIdContext;

    public GridLoadSimulator(CommandBus commandBus, CorrelationIdContext correlationIdContext) {
        this.commandBus = commandBus;
        this.correlationIdContext = correlationIdContext;
    }

    @Scheduled(fixedDelayString = "${aetherstream.simulation.interval-ms:5000}")
    public void emitGridLoad() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String region = REGIONS.get(random.nextInt(REGIONS.size()));
        double demand = 400 + random.nextDouble(0, 200);
        double supply = demand - 20 + random.nextDouble(0, 80);
        var gridLoad = new GridLoad(region, demand, supply, Instant.now());
        String correlationId = correlationIdContext.getOrCreate();
        commandBus.dispatch(new RecordGridLoadCommand(gridLoad, correlationId));
        log.info("Simulated grid load for {} (correlationId={})", region, correlationId);
    }
}
