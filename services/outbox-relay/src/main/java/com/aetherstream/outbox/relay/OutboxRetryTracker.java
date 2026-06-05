package com.aetherstream.outbox.relay;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Tracks publish retry attempts per outbox event across poll cycles. */
@Component
class OutboxRetryTracker {

    private final ConcurrentHashMap<UUID, Integer> attempts = new ConcurrentHashMap<>();

    int increment(UUID eventId) {
        return attempts.merge(eventId, 1, Integer::sum);
    }

    void clear(UUID eventId) {
        attempts.remove(eventId);
    }
}
