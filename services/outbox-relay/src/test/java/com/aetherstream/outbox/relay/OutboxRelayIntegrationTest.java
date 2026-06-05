package com.aetherstream.outbox.relay;

import static org.assertj.core.api.Assertions.assertThat;

import com.aetherstream.domain.event.AggregateTypes;
import com.aetherstream.domain.event.EventTypes;
import com.aetherstream.domain.event.Topics;
import com.aetherstream.domain.outbox.OutboxEvent;
import com.aetherstream.domain.outbox.OutboxStatus;
import com.aetherstream.infrastructure.persistence.OutboxEventMapper;
import com.aetherstream.infrastructure.persistence.repository.OutboxEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = OutboxRelayApplication.class)
@Testcontainers
class OutboxRelayIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("aetherstream")
            .withUsername("aether")
            .withPassword("aether");

    @Container
    static KafkaContainer kafka = new KafkaContainer();

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("aetherstream.outbox.poll-interval-ms", () -> "60000");
    }

    @Autowired
    private OutboxRelayService relayService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUpConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafka.getBootstrapServers(), "outbox-relay-it", "true");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumer = new DefaultKafkaConsumerFactory<>(
                        consumerProps, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        consumer.subscribe(java.util.List.of(Topics.TURBINE_EVENTS));
    }

    @Test
    void relayBatch_publishesPendingOutboxRowToKafkaTopic() {
        UUID eventId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();
        String payload =
                """
                {"eventId":"%s","eventType":"%s","occurredAt":"2026-06-05T12:00:00Z",\
                "correlationId":"%s","payload":{"turbineId":"T-RELAY-001","rpm":10.0,\
                "powerOutput":1200.0,"vibrationLevel":0.2,"recordedAt":"2026-06-05T12:00:00Z"}}
                """
                        .formatted(eventId, EventTypes.TURBINE_TELEMETRY_RECORDED, correlationId);

        OutboxEvent pending = new OutboxEvent(
                eventId,
                AggregateTypes.TURBINE,
                "T-RELAY-001",
                EventTypes.TURBINE_TELEMETRY_RECORDED,
                payload,
                OutboxStatus.PENDING,
                Instant.parse("2026-06-05T12:00:00Z"),
                null);
        outboxEventRepository.save(OutboxEventMapper.toEntity(pending));

        int sent = relayService.relayBatch();

        assertThat(sent).isEqualTo(1);

        var updated = outboxEventRepository.findById(eventId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(updated.getProcessedAt()).isNotNull();

        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(consumer, Topics.TURBINE_EVENTS, Duration.ofSeconds(10));
        assertThat(record.key()).isEqualTo("T-RELAY-001");
        assertThat(record.value()).contains(correlationId);
        assertThat(record.value()).contains("T-RELAY-001");

        var correlationHeader = record.headers().lastHeader("X-Correlation-Id");
        assertThat(correlationHeader).isNotNull();
        assertThat(new String(correlationHeader.value(), java.nio.charset.StandardCharsets.UTF_8))
                .isEqualTo(correlationId);
    }
}
