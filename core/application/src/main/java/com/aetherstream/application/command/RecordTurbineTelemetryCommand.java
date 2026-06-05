package com.aetherstream.application.command;

import com.aetherstream.application.cqrs.Command;
import com.aetherstream.domain.model.Turbine;

/**
 * Command to record turbine telemetry: upserts the write model and appends an outbox event
 * in a single transaction.
 */
public record RecordTurbineTelemetryCommand(Turbine turbine, String correlationId) implements Command {
}
