package com.aetherstream.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.aetherstream.domain.event.EventTypes;
import com.aetherstream.domain.event.Topics;
import com.aetherstream.infrastructure.correlation.CorrelationIdContext;
import com.aetherstream.infrastructure.persistence.repository.EnergyStateSnapshotRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = ApiGatewayApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CorrelationIdPropagationIntegrationTest {

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
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }

    @Autowired
    private EnergyStateSnapshotRepository energyStateSnapshotRepository;

    @Test
    void kafkaConsumer_projectsEventWhenCorrelationIdArrivesViaHeader() {
        UUID energyEventId = UUID.randomUUID();
        String correlationId = "corr-e2e-" + UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-06-05T15:00:00Z");

        String energyJson =
                """
                {"eventId":"%s","eventType":"%s","occurredAt":"%s","correlationId":"%s",\
                "payload":{"region":"baltic","totalWindPower":3100.0,"gridDemand":2900.0,\
                "efficiencyScore":0.88,"timestamp":"%s"}}
                """
                        .formatted(
                                energyEventId,
                                EventTypes.ENERGY_STATE_COMPUTED,
                                timestamp,
                                correlationId,
                                timestamp);

        var record = new ProducerRecord<>(Topics.ENERGY_STATE_EVENTS, "baltic", energyJson);
        record.headers().add(new RecordHeader(
                CorrelationIdContext.HEADER, correlationId.getBytes(StandardCharsets.UTF_8)));

        KafkaTemplate<String, String> producer = kafkaProducer();
        producer.send(record);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(energyStateSnapshotRepository.findById(energyEventId)).isPresent();
        });

        var entity = energyStateSnapshotRepository.findById(energyEventId).orElseThrow();
        assertThat(entity.getRegion()).isEqualTo("baltic");
        assertThat(entity.getTotalWindPower()).isEqualTo(3100.0);
    }

    private KafkaTemplate<String, String> kafkaProducer() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }
}
