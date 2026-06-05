package com.aetherstream.infrastructure.messaging;

import com.aetherstream.domain.event.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declarative Kafka topic definitions. When a Kafka broker is available, Spring's
 * {@code KafkaAdmin} reconciles these {@link NewTopic} beans on startup. Local defaults use
 * a single partition and replication factor 1; production would override both.
 */
@Configuration
public class KafkaTopicsConfig {

    private static final int PARTITIONS = 1;
    private static final short REPLICAS = 1;

    @Bean
    NewTopic weatherEventsTopic() {
        return TopicBuilder.name(Topics.WEATHER_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic turbineEventsTopic() {
        return TopicBuilder.name(Topics.TURBINE_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic gridEventsTopic() {
        return TopicBuilder.name(Topics.GRID_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic energyStateEventsTopic() {
        return TopicBuilder.name(Topics.ENERGY_STATE_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic alertsTopic() {
        return TopicBuilder.name(Topics.ALERTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic deadLetterEventsTopic() {
        return TopicBuilder.name(Topics.DEAD_LETTER_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic outboxEventsTopic() {
        return TopicBuilder.name(Topics.OUTBOX_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }
}
