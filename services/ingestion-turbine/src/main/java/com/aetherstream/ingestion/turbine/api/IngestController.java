package com.aetherstream.ingestion.turbine.api;

import com.aetherstream.application.command.RecordTurbineTelemetryCommand;
import com.aetherstream.application.cqrs.CommandBus;
import com.aetherstream.domain.model.Turbine;
import com.aetherstream.domain.outbox.OutboxStatus;
import com.aetherstream.infrastructure.correlation.CorrelationIdContext;
import com.aetherstream.infrastructure.persistence.repository.OutboxEventRepository;
import com.aetherstream.infrastructure.web.dto.IngestResponse;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingest")
public class IngestController {

    private final CommandBus commandBus;
    private final CorrelationIdContext correlationIdContext;
    private final OutboxEventRepository outboxEventRepository;

    public IngestController(
            CommandBus commandBus,
            CorrelationIdContext correlationIdContext,
            OutboxEventRepository outboxEventRepository) {
        this.commandBus = commandBus;
        this.correlationIdContext = correlationIdContext;
        this.outboxEventRepository = outboxEventRepository;
    }

    @PostMapping("/turbine")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IngestResponse ingestTurbine(@RequestBody TurbineIngestRequest request) {
        String correlationId = correlationIdContext.getOrCreate();
        Instant timestamp = request.timestamp() != null ? request.timestamp() : Instant.now();
        var turbine = new Turbine(
                request.turbineId(), request.rpm(), request.powerOutput(), request.vibrationLevel(), timestamp);
        commandBus.dispatch(new RecordTurbineTelemetryCommand(turbine, correlationId));

        var event = outboxEventRepository
                .findFirstByAggregateIdAndStatusOrderByCreatedAtDesc(turbine.turbineId(), OutboxStatus.PENDING)
                .orElseThrow();

        return new IngestResponse(event.getId(), correlationId, OutboxStatus.PENDING.name());
    }
}
