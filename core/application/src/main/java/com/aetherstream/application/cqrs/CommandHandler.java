package com.aetherstream.application.cqrs;

/**
 * Handles a single type of {@link Command}. Implementations live in the write-side
 * services and perform domain validation plus persistence (including the outbox write).
 *
 * @param <C> the command type handled
 */
public interface CommandHandler<C extends Command> {

    void handle(C command);

    Class<C> commandType();
}
