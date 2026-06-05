package com.aetherstream.outbox.relay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Outbox relay service. On a schedule it polls PENDING {@code outbox_events}, publishes them
 * to Kafka (at-least-once, batched, with retries), marks them SENT, and routes exhausted
 * failures to the dead-letter topic.
 *
 * <p>The polling/publishing logic is implemented in the relay phase.
 */
@SpringBootApplication(scanBasePackages = "com.aetherstream")
@EntityScan(basePackages = "com.aetherstream.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "com.aetherstream.infrastructure.persistence.repository")
@EnableScheduling
public class OutboxRelayApplication {

    public static void main(String[] args) {
        SpringApplication.run(OutboxRelayApplication.class, args);
    }
}
