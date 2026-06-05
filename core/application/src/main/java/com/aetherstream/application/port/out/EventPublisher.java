package com.aetherstream.application.port.out;

import com.aetherstream.domain.outbox.OutboxEvent;

/**
 * Outbound port used by the outbox relay to publish an event to the messaging backbone.
 * Delivery is at-least-once; the event id is the downstream idempotency key.
 */
public interface EventPublisher {

    void publish(String topic, String key, OutboxEvent event);
}
