package com.aetherstream.application.cqrs;

/**
 * Dispatches a {@link Command} to its registered {@link CommandHandler}. This is the
 * write-side analogue of a mediator; it decouples callers from concrete handlers.
 */
public interface CommandBus {

    <C extends Command> void dispatch(C command);
}
