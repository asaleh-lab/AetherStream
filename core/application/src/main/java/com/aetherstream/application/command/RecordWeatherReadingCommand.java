package com.aetherstream.application.command;

import com.aetherstream.application.cqrs.Command;
import com.aetherstream.domain.model.WeatherReading;

/** Command to record a weather reading via the transactional outbox. */
public record RecordWeatherReadingCommand(WeatherReading reading, String correlationId) implements Command {
}
