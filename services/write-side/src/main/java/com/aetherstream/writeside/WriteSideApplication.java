package com.aetherstream.writeside;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Write-side service: ingest REST APIs, CQRS command handlers, PostgreSQL write model, and
 * transactional outbox. Data source simulators POST here; outbox-relay publishes to Kafka.
 */
@SpringBootApplication(scanBasePackages = "com.aetherstream")
@EntityScan(basePackages = "com.aetherstream.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "com.aetherstream.infrastructure.persistence.repository")
public class WriteSideApplication {

    public static void main(String[] args) {
        SpringApplication.run(WriteSideApplication.class, args);
    }
}
