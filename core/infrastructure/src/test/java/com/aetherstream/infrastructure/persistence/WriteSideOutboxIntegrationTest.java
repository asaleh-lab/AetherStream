package com.aetherstream.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.aetherstream.application.command.RecordTurbineTelemetryCommand;
import com.aetherstream.application.cqrs.CommandBus;
import com.aetherstream.domain.event.AggregateTypes;
import com.aetherstream.domain.event.EventTypes;
import com.aetherstream.domain.model.Turbine;
import com.aetherstream.domain.outbox.OutboxStatus;
import com.aetherstream.infrastructure.WriteSideTestApplication;
import com.aetherstream.infrastructure.persistence.entity.TurbineStateEntity;
import com.aetherstream.infrastructure.persistence.repository.OutboxEventRepository;
import com.aetherstream.infrastructure.persistence.repository.TurbineStateRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = WriteSideTestApplication.class)
@Testcontainers
class WriteSideOutboxIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("aetherstream")
            .withUsername("aether")
            .withPassword("aether");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TurbineStateRepository turbineStateRepository;

    @Test
    void dispatch_writesTurbineStateAndPendingOutboxRowInOneTransaction() {
        String correlationId = UUID.randomUUID().toString();
        var turbine = new Turbine("T-IT-001", 12.5, 1500.0, 0.4, Instant.parse("2026-06-05T12:00:00Z"));

        commandBus.dispatch(new RecordTurbineTelemetryCommand(turbine, correlationId));

        TurbineStateEntity state = turbineStateRepository.findById("T-IT-001").orElseThrow();
        assertThat(state.getRpm()).isEqualTo(12.5);
        assertThat(state.getPowerOutput()).isEqualTo(1500.0);
        assertThat(state.getVibrationLevel()).isEqualTo(0.4);

        var outbox = outboxEventRepository
                .findFirstByAggregateIdAndStatusOrderByCreatedAtDesc("T-IT-001", OutboxStatus.PENDING)
                .orElseThrow();
        assertThat(outbox.getAggregateType()).isEqualTo(AggregateTypes.TURBINE);
        assertThat(outbox.getEventType()).isEqualTo(EventTypes.TURBINE_TELEMETRY_RECORDED);
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getPayload()).contains(correlationId);
        assertThat(outbox.getPayload()).contains("T-IT-001");
    }
}
