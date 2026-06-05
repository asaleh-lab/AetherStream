package com.aetherstream.datasource.turbine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Thin producer: simulates turbine telemetry and POSTs JSON to the write-side service. */
@SpringBootApplication
@EnableScheduling
public class DatasourceTurbineApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatasourceTurbineApplication.class, args);
    }
}
