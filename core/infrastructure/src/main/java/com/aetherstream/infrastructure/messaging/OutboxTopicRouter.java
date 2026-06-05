package com.aetherstream.infrastructure.messaging;

import com.aetherstream.domain.event.AggregateTypes;
import com.aetherstream.domain.event.Topics;
import org.springframework.stereotype.Component;

/**
 * Maps outbox aggregate types to their destination Kafka topics.
 */
@Component
public class OutboxTopicRouter {

    public String topicFor(String aggregateType) {
        return switch (aggregateType) {
            case AggregateTypes.TURBINE -> Topics.TURBINE_EVENTS;
            case AggregateTypes.GRID_LOAD -> Topics.GRID_EVENTS;
            default -> throw new IllegalArgumentException("Unknown aggregate type for relay: " + aggregateType);
        };
    }
}
