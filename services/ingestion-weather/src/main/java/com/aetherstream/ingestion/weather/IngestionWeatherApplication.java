package com.aetherstream.ingestion.weather;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Weather ingestion service. Polls an external weather API on a schedule, converts readings
 * into commands, and persists state plus an outbox event in a single transaction.
 *
 * <p>Business logic (polling, command handlers) is added in the write-side phase.
 */
@SpringBootApplication(scanBasePackages = "com.aetherstream")
@EntityScan(basePackages = "com.aetherstream.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "com.aetherstream.infrastructure.persistence.repository")
@EnableScheduling
public class IngestionWeatherApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestionWeatherApplication.class, args);
    }
}
