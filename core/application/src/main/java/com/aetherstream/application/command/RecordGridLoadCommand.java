package com.aetherstream.application.command;

import com.aetherstream.application.cqrs.Command;
import com.aetherstream.domain.model.GridLoad;

/** Command to record a grid load reading via the transactional outbox. */
public record RecordGridLoadCommand(GridLoad gridLoad, String correlationId) implements Command {
}
