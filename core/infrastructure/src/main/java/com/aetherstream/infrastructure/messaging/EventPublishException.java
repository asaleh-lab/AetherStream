package com.aetherstream.infrastructure.messaging;

/** Raised when the outbox relay cannot publish an event to Kafka. */
public class EventPublishException extends RuntimeException {

    public EventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
