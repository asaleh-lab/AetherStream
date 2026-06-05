package com.aetherstream.application.handler;

import com.aetherstream.application.command.RecordWeatherReadingCommand;
import com.aetherstream.application.cqrs.CommandHandler;
import com.aetherstream.application.port.out.OutboxWriter;
import com.aetherstream.domain.event.AggregateTypes;
import com.aetherstream.domain.event.EventTypes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecordWeatherReadingHandler implements CommandHandler<RecordWeatherReadingCommand> {

    private final OutboxWriter outboxWriter;

    public RecordWeatherReadingHandler(OutboxWriter outboxWriter) {
        this.outboxWriter = outboxWriter;
    }

    @Override
    public void handle(RecordWeatherReadingCommand command) {
        var reading = command.reading();
        outboxWriter.writePending(
                AggregateTypes.WEATHER_READING,
                reading.region(),
                EventTypes.WEATHER_READING_RECORDED,
                reading,
                command.correlationId());
    }

    @Override
    public Class<RecordWeatherReadingCommand> commandType() {
        return RecordWeatherReadingCommand.class;
    }
}
