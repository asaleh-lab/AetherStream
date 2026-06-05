package com.aetherstream.outbox.relay;

import com.aetherstream.infrastructure.config.OutboxRelayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OutboxRelayProperties.class)
public class OutboxRelayConfiguration {
}
