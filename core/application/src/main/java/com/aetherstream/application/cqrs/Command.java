package com.aetherstream.application.cqrs;

/**
 * Marker interface for write-side commands. A command expresses an intent to change state
 * and is handled by exactly one {@link CommandHandler}.
 */
public interface Command {
}
