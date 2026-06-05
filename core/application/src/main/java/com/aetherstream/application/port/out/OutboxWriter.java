package com.aetherstream.application.port.out;

/**
 * Outbound port for writing a PENDING outbox event with a versioned JSON envelope. The
 * implementation MUST enlist in the caller's transaction.
 */
public interface OutboxWriter {

    void writePending(
            String aggregateType,
            String aggregateId,
            String eventType,
            Object payloadBody,
            String correlationId);
}
