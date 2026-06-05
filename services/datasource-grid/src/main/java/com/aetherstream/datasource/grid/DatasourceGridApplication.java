package com.aetherstream.datasource.grid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Thin producer: simulates grid load and POSTs JSON to the write-side service. */
@SpringBootApplication
@EnableScheduling
public class DatasourceGridApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatasourceGridApplication.class, args);
    }
}
