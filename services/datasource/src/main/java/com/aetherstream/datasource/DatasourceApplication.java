package com.aetherstream.datasource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Thin producer for simulated external feeds. No DB, domain, or CQRS — schedules two independent
 * simulators (turbine telemetry, grid load) at real-world intervals and POSTs JSON to the
 * write-side ingest API.
 */
@SpringBootApplication
@EnableScheduling
public class DatasourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatasourceApplication.class, args);
    }
}
