package com.aetherstream.outbox.relay;

import com.aetherstream.application.port.out.EventPublisher;
import com.aetherstream.domain.event.DeadLetterEnvelope;
import com.aetherstream.domain.event.Topics;
import com.aetherstream.domain.outbox.OutboxEvent;
import com.aetherstream.domain.outbox.OutboxStatus;
import com.aetherstream.infrastructure.config.OutboxRelayProperties;
import com.aetherstream.infrastructure.messaging.EventPublishException;
import com.aetherstream.infrastructure.messaging.OutboxTopicRouter;
import com.aetherstream.infrastructure.persistence.OutboxEventMapper;
import com.aetherstream.infrastructure.persistence.entity.OutboxEventEntity;
import com.aetherstream.infrastructure.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls PENDING outbox rows, publishes them to Kafka, and updates lifecycle status.
 * Failed publishes are retried across poll cycles; exhausted retries route to the DLQ.
 */
@Service
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final EventPublisher eventPublisher;
    private final OutboxTopicRouter topicRouter;
    private final OutboxRelayProperties properties;
    private final OutboxRetryTracker retryTracker;
    private final ObjectMapper objectMapper;

    public OutboxRelayService(
            OutboxEventRepository outboxEventRepository,
            EventPublisher eventPublisher,
            OutboxTopicRouter topicRouter,
            OutboxRelayProperties properties,
            OutboxRetryTracker retryTracker,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventPublisher = eventPublisher;
        this.topicRouter = topicRouter;
        this.properties = properties;
        this.retryTracker = retryTracker;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int relayBatch() {
        List<OutboxEventEntity> batch = outboxEventRepository.lockPendingBatch(properties.batchSize());
        if (batch.isEmpty()) {
            return 0;
        }

        int sent = 0;
        Instant now = Instant.now();

        for (OutboxEventEntity entity : batch) {
            OutboxEvent event = OutboxEventMapper.toDomain(entity);
            String topic = topicRouter.topicFor(event.aggregateType());

            try {
                eventPublisher.publish(topic, event.aggregateId(), event);
                entity.setStatus(OutboxStatus.SENT);
                entity.setProcessedAt(now);
                retryTracker.clear(event.id());
                sent++;
            } catch (EventPublishException ex) {
                handlePublishFailure(entity, event, topic, ex, now);
            }
        }

        outboxEventRepository.saveAll(batch);
        if (sent > 0) {
            log.info("Relayed {} of {} outbox events", sent, batch.size());
        }
        return sent;
    }

    private void handlePublishFailure(
            OutboxEventEntity entity, OutboxEvent event, String topic, EventPublishException ex, Instant now) {
        int attempts = retryTracker.increment(event.id());
        log.warn(
                "Failed to publish outbox event {} (attempt {}/{}): {}",
                event.id(),
                attempts,
                properties.maxRetries(),
                ex.getMessage());

        if (attempts >= properties.maxRetries()) {
            publishToDeadLetter(event, topic, ex.getMessage());
            entity.setStatus(OutboxStatus.FAILED);
            entity.setProcessedAt(now);
            retryTracker.clear(event.id());
            log.error("Outbox event {} moved to DLQ after {} failed attempts", event.id(), attempts);
        }
    }

    private void publishToDeadLetter(OutboxEvent event, String originalTopic, String errorMessage) {
        var dlqEnvelope = new DeadLetterEnvelope(
                event.id(),
                originalTopic,
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                event.payload(),
                errorMessage,
                Instant.now());
        try {
            String payload = objectMapper.writeValueAsString(dlqEnvelope);
            var dlqEvent = new OutboxEvent(
                    event.id(),
                    event.aggregateType(),
                    event.aggregateId(),
                    event.eventType(),
                    payload,
                    event.status(),
                    event.createdAt(),
                    event.processedAt());
            eventPublisher.publish(Topics.DEAD_LETTER_EVENTS, event.aggregateId(), dlqEvent);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize DLQ envelope for event " + event.id(), e);
        } catch (EventPublishException e) {
            log.error("Could not publish DLQ envelope for outbox event {}", event.id(), e);
        }
    }
}
