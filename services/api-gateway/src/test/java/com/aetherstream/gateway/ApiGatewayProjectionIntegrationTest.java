package com.aetherstream.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.aetherstream.domain.event.EventTypes;
import com.aetherstream.domain.event.Topics;
import com.aetherstream.domain.model.AlertType;
import com.aetherstream.domain.model.Severity;
import com.aetherstream.infrastructure.persistence.repository.AlertRepository;
import com.aetherstream.infrastructure.persistence.repository.EnergyStateSnapshotRepository;
import com.aetherstream.infrastructure.persistence.repository.RecommendationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
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
class ApiGatewayProjectionIntegrationTest {

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

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void kafkaConsumers_projectEnergyStateAndAlertsIntoReadModels() {
        UUID energyEventId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();
        Instant timestamp = Instant.parse("2026-06-05T14:00:00Z");

        String energyJson =
                """
                {"eventId":"%s","eventType":"%s","occurredAt":"%s","correlationId":"%s",\
                "payload":{"region":"north-sea","totalWindPower":4200.0,"gridDemand":3800.0,\
                "efficiencyScore":0.91,"timestamp":"%s"}}
                """
                        .formatted(
                                energyEventId,
                                EventTypes.ENERGY_STATE_COMPUTED,
                                timestamp,
                                correlationId,
                                timestamp);

        UUID alertEventId = UUID.randomUUID();
        String alertJson =
                """
                {"eventId":"%s","eventType":"%s","occurredAt":"%s","correlationId":"%s",\
                "payload":{"id":"%s","type":"VIBRATION_SPIKE","severity":"WARNING",\
                "source":"T-001","message":"Vibration spike detected","timestamp":"%s"}}
                """
                        .formatted(
                                alertEventId,
                                EventTypes.ALERT_RAISED,
                                timestamp,
                                correlationId,
                                alertEventId,
                                timestamp);

        KafkaTemplate<String, String> producer = kafkaProducer();
        producer.send(Topics.ENERGY_STATE_EVENTS, "north-sea", energyJson);
        producer.send(Topics.ALERTS, "T-001", alertJson);

        UUID recommendationEventId = UUID.randomUUID();
        String recommendationJson =
                """
                {"eventId":"%s","eventType":"%s","occurredAt":"%s","correlationId":"%s",\
                "payload":{"id":"%s","region":"north-sea",\
                "suggestion":"Increase turbine output in north-sea","timestamp":"%s"}}
                """
                        .formatted(
                                recommendationEventId,
                                EventTypes.RECOMMENDATION_ISSUED,
                                timestamp,
                                correlationId,
                                recommendationEventId,
                                timestamp);
        producer.send(Topics.RECOMMENDATIONS, "north-sea", recommendationJson);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(energyStateSnapshotRepository.findById(energyEventId)).isPresent();
            assertThat(alertRepository.findById(alertEventId)).isPresent();
            assertThat(recommendationRepository.findById(recommendationEventId)).isPresent();
        });

        var energyEntity = energyStateSnapshotRepository.findById(energyEventId).orElseThrow();
        assertThat(energyEntity.getRegion()).isEqualTo("north-sea");
        assertThat(energyEntity.getTotalWindPower()).isEqualTo(4200.0);

        var alertEntity = alertRepository.findById(alertEventId).orElseThrow();
        assertThat(alertEntity.getType()).isEqualTo(AlertType.VIBRATION_SPIKE);
        assertThat(alertEntity.getSeverity()).isEqualTo(Severity.WARNING);

        var response = restTemplate.getForEntity("/api/alerts?limit=10", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("T-001");

        var recommendationsResponse = restTemplate.getForEntity("/api/recommendations?limit=10", String.class);
        assertThat(recommendationsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(recommendationsResponse.getBody()).contains("north-sea");
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
