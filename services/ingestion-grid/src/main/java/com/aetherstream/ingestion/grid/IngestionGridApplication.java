package com.aetherstream.ingestion.grid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Grid ingestion service. Simulates a real-time grid load feed and records it via commands,
 * persisting state plus an outbox event in a single transaction.
 *
 * <p>Feed simulation and command handlers are added in the write-side phase.
 */
@SpringBootApplication(scanBasePackages = "com.aetherstream")
@EntityScan(basePackages = "com.aetherstream.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "com.aetherstream.infrastructure.persistence.repository")
public class IngestionGridApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestionGridApplication.class, args);
    }
}
