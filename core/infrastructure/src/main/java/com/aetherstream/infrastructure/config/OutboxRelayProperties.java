package com.aetherstream.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aetherstream.outbox")
public record OutboxRelayProperties(long pollIntervalMs, int batchSize, int maxRetries) {
}
