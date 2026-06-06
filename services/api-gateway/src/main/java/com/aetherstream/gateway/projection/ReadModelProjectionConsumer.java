package com.aetherstream.gateway.projection;

import com.aetherstream.application.port.out.AlertReadModel;
import com.aetherstream.application.port.out.EnergyStateReadModel;
import com.aetherstream.application.port.out.RecommendationReadModel;
import com.aetherstream.domain.event.EventTypes;
import com.aetherstream.domain.model.Alert;
import com.aetherstream.domain.model.EnergyState;
import com.aetherstream.domain.model.Recommendation;
import com.aetherstream.domain.model.Turbine;
import com.aetherstream.gateway.config.KafkaConsumerConfiguration;
import com.aetherstream.gateway.realtime.RealtimeWebSocketHandler;
import com.aetherstream.infrastructure.correlation.CorrelationIdContext;
import com.aetherstream.infrastructure.messaging.EventEnvelopeParser;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReadModelProjectionConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReadModelProjectionConsumer.class);

    private final EventEnvelopeParser envelopeParser;
    private final EnergyStateReadModel energyStateReadModel;
    private final AlertReadModel alertReadModel;
    private final RecommendationReadModel recommendationReadModel;
    private final RealtimeWebSocketHandler realtimeWebSocketHandler;
    private final CorrelationIdContext correlationIdContext;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public ReadModelProjectionConsumer(
            EventEnvelopeParser envelopeParser,
            EnergyStateReadModel energyStateReadModel,
            AlertReadModel alertReadModel,
            RecommendationReadModel recommendationReadModel,
            RealtimeWebSocketHandler realtimeWebSocketHandler,
            CorrelationIdContext correlationIdContext,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.envelopeParser = envelopeParser;
        this.energyStateReadModel = energyStateReadModel;
        this.alertReadModel = alertReadModel;
        this.recommendationReadModel = recommendationReadModel;
        this.realtimeWebSocketHandler = realtimeWebSocketHandler;
        this.correlationIdContext = correlationIdContext;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = KafkaConsumerConfiguration.ENERGY_STATE_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onEnergyState(ConsumerRecord<String, String> record) {
        withCorrelation(record, () -> {
            var envelope = envelopeParser.parse(record.value());
            if (!EventTypes.ENERGY_STATE_COMPUTED.equals(envelope.eventType())) {
                log.debug("Skipping unexpected event type on energy-state topic: {}", envelope.eventType());
                return;
            }
            var state = (EnergyState) envelope.payload();
            energyStateReadModel.upsert(envelope.eventId(), state);
            realtimeWebSocketHandler.broadcast("energy-state", state);
            log.debug("Projected energy state for region {}", state.region());
        });
    }

    @KafkaListener(
            topics = KafkaConsumerConfiguration.TURBINE_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onTurbine(ConsumerRecord<String, String> record) {
        withCorrelation(record, () -> {
            var envelope = envelopeParser.parse(record.value());
            if (!EventTypes.TURBINE_TELEMETRY_RECORDED.equals(envelope.eventType())) {
                log.debug("Skipping unexpected event type on turbine topic: {}", envelope.eventType());
                return;
            }
            var turbine = (Turbine) envelope.payload();
            realtimeWebSocketHandler.broadcast("turbine", turbine);
            log.debug("Broadcast turbine telemetry for {}", turbine.turbineId());
        });
    }

    @KafkaListener(
            topics = KafkaConsumerConfiguration.ALERTS_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onAlert(ConsumerRecord<String, String> record) {
        withCorrelation(record, () -> {
            var envelope = envelopeParser.parse(record.value());
            if (!EventTypes.ALERT_RAISED.equals(envelope.eventType())) {
                log.debug("Skipping unexpected event type on alerts topic: {}", envelope.eventType());
                return;
            }
            var alert = (Alert) envelope.payload();
            alertReadModel.upsert(envelope.eventId(), alert);
            realtimeWebSocketHandler.broadcast("alert", alert);
            log.debug("Projected alert {} from {}", alert.type(), alert.source());
        });
    }

    @KafkaListener(
            topics = KafkaConsumerConfiguration.RECOMMENDATIONS_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onRecommendation(ConsumerRecord<String, String> record) {
        withCorrelation(record, () -> {
            var envelope = envelopeParser.parse(record.value());
            if (!EventTypes.RECOMMENDATION_ISSUED.equals(envelope.eventType())) {
                log.debug("Skipping unexpected event type on recommendations topic: {}", envelope.eventType());
                return;
            }
            var recommendation = (Recommendation) envelope.payload();
            recommendationReadModel.upsert(envelope.eventId(), recommendation);
            realtimeWebSocketHandler.broadcast("recommendation", recommendation);
            log.debug("Projected recommendation for region {}", recommendation.region());
        });
    }

    private void withCorrelation(ConsumerRecord<String, String> record, Runnable action) {
        String correlationId = extractCorrelationId(record);
        MDC.put(CorrelationIdContext.MDC_KEY, correlationId);
        correlationIdContext.set(correlationId);
        try {
            action.run();
        } finally {
            MDC.remove(CorrelationIdContext.MDC_KEY);
        }
    }

    private String extractCorrelationId(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader(CorrelationIdContext.HEADER);
        if (header != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        try {
            return objectMapper.readTree(record.value()).get("correlationId").asText();
        } catch (Exception e) {
            return java.util.UUID.randomUUID().toString();
        }
    }
}
