package com.aetherstream.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * API gateway. Serves read-side query APIs over REST and pushes energy-state and alert
 * updates to the Blazor UI over WebSocket.
 *
 * <p>Query handlers, controllers, Kafka projections, and the WebSocket endpoint are added
 * in the query-side phase.
 */
@SpringBootApplication(scanBasePackages = "com.aetherstream")
@EntityScan(basePackages = "com.aetherstream.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "com.aetherstream.infrastructure.persistence.repository")
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
