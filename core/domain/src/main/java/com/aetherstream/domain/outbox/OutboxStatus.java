package com.aetherstream.domain.outbox;

/** Lifecycle state of an outbox event row. */
public enum OutboxStatus {
    /** Written in the business transaction; awaiting publication. */
    PENDING,
    /** Successfully published to Kafka. */
    SENT,
    /** Publication failed after exhausting retries; routed to the dead-letter topic. */
    FAILED
}
