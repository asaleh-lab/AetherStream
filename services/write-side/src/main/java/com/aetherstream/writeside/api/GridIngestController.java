package com.aetherstream.writeside.api;

import com.aetherstream.application.command.RecordGridLoadCommand;
import com.aetherstream.application.cqrs.CommandBus;
import com.aetherstream.domain.model.GridLoad;
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
public class GridIngestController {

    private final CommandBus commandBus;
    private final CorrelationIdContext correlationIdContext;
    private final OutboxEventRepository outboxEventRepository;

    public GridIngestController(
            CommandBus commandBus,
            CorrelationIdContext correlationIdContext,
            OutboxEventRepository outboxEventRepository) {
        this.commandBus = commandBus;
        this.correlationIdContext = correlationIdContext;
        this.outboxEventRepository = outboxEventRepository;
    }

    @PostMapping("/grid")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IngestResponse ingestGrid(@RequestBody GridIngestRequest request) {
        String correlationId = correlationIdContext.getOrCreate();
        Instant timestamp = request.timestamp() != null ? request.timestamp() : Instant.now();
        var gridLoad = new GridLoad(request.region(), request.demandMW(), request.supplyMW(), timestamp);
        commandBus.dispatch(new RecordGridLoadCommand(gridLoad, correlationId));

        var event = outboxEventRepository
                .findFirstByAggregateIdAndStatusOrderByCreatedAtDesc(gridLoad.region(), OutboxStatus.PENDING)
                .orElseThrow();

        return new IngestResponse(event.getId(), correlationId, OutboxStatus.PENDING.name());
    }
}
