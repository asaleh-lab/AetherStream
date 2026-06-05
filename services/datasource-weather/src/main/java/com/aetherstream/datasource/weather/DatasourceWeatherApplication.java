package com.aetherstream.datasource.weather;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Thin producer: polls an external weather API and POSTs JSON to the write-side service. */
@SpringBootApplication
@EnableScheduling
public class DatasourceWeatherApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatasourceWeatherApplication.class, args);
    }
}
