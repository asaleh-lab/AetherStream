package com.aetherstream.application.port.out;

import com.aetherstream.domain.outbox.OutboxEvent;

/**
 * Outbound port for appending an event to the transactional outbox. Implementations MUST
 * enlist in the same transaction as the domain state change so that the state write and the
 * outbox write commit atomically (no dual-write).
 */
public interface OutboxAppender {

    void append(OutboxEvent event);
}
