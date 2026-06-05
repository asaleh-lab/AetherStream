package com.aetherstream.infrastructure.messaging;

import com.aetherstream.application.port.out.EventPublisher;
import com.aetherstream.domain.event.EventEnvelope;
import com.aetherstream.domain.outbox.OutboxEvent;
import com.aetherstream.infrastructure.correlation.CorrelationIdContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes outbox events to Kafka with keyed, idempotent producer semantics and correlation headers.
 */
@Service
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String EVENT_ID_HEADER = "eventId";
    private static final long SEND_TIMEOUT_SECONDS = 30;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String topic, String key, OutboxEvent event) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, event.payload());
        record.headers().add(new RecordHeader(EVENT_ID_HEADER, event.id().toString().getBytes(StandardCharsets.UTF_8)));

        String correlationId = extractCorrelationId(event.payload());
        if (correlationId != null) {
            record.headers()
                    .add(new RecordHeader(
                            CorrelationIdContext.HEADER, correlationId.getBytes(StandardCharsets.UTF_8)));
        }

        try {
            kafkaTemplate.send(record).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.debug("Published outbox event {} to topic {}", event.id(), topic);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventPublishException("Interrupted while publishing event " + event.id(), e);
        } catch (ExecutionException | TimeoutException e) {
            throw new EventPublishException("Failed to publish event " + event.id() + " to " + topic, e);
        }
    }

    private String extractCorrelationId(String payloadJson) {
        try {
            EventEnvelope envelope = objectMapper.readValue(payloadJson, EventEnvelope.class);
            return envelope.correlationId();
        } catch (Exception e) {
            log.warn("Could not parse correlation id from outbox payload for relay header propagation");
            return null;
        }
    }
}
