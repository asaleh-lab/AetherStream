package com.aetherstream.application.handler;

import com.aetherstream.application.command.RecordTurbineTelemetryCommand;
import com.aetherstream.application.cqrs.CommandHandler;
import com.aetherstream.application.port.out.OutboxWriter;
import com.aetherstream.application.port.out.TurbineStateStore;
import com.aetherstream.domain.event.AggregateTypes;
import com.aetherstream.domain.event.EventTypes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecordTurbineTelemetryHandler implements CommandHandler<RecordTurbineTelemetryCommand> {

    private final TurbineStateStore turbineStateStore;
    private final OutboxWriter outboxWriter;

    public RecordTurbineTelemetryHandler(TurbineStateStore turbineStateStore, OutboxWriter outboxWriter) {
        this.turbineStateStore = turbineStateStore;
        this.outboxWriter = outboxWriter;
    }

    @Override
    public void handle(RecordTurbineTelemetryCommand command) {
        var turbine = command.turbine();
        turbineStateStore.upsert(turbine);
        outboxWriter.writePending(
                AggregateTypes.TURBINE,
                turbine.turbineId(),
                EventTypes.TURBINE_TELEMETRY_RECORDED,
                turbine,
                command.correlationId());
    }

    @Override
    public Class<RecordTurbineTelemetryCommand> commandType() {
        return RecordTurbineTelemetryCommand.class;
    }
}
