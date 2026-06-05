package com.aetherstream.application.handler;

import com.aetherstream.application.command.RecordGridLoadCommand;
import com.aetherstream.application.cqrs.CommandHandler;
import com.aetherstream.application.port.out.OutboxWriter;
import com.aetherstream.domain.event.AggregateTypes;
import com.aetherstream.domain.event.EventTypes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecordGridLoadHandler implements CommandHandler<RecordGridLoadCommand> {

    private final OutboxWriter outboxWriter;

    public RecordGridLoadHandler(OutboxWriter outboxWriter) {
        this.outboxWriter = outboxWriter;
    }

    @Override
    public void handle(RecordGridLoadCommand command) {
        var gridLoad = command.gridLoad();
        outboxWriter.writePending(
                AggregateTypes.GRID_LOAD,
                gridLoad.region(),
                EventTypes.GRID_LOAD_RECORDED,
                gridLoad,
                command.correlationId());
    }

    @Override
    public Class<RecordGridLoadCommand> commandType() {
        return RecordGridLoadCommand.class;
    }
}
