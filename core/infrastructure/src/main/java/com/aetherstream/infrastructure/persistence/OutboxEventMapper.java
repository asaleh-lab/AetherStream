package com.aetherstream.infrastructure.persistence;

import com.aetherstream.domain.outbox.OutboxEvent;
import com.aetherstream.infrastructure.persistence.entity.OutboxEventEntity;

public final class OutboxEventMapper {

    private OutboxEventMapper() {
    }

    public static OutboxEventEntity toEntity(OutboxEvent event) {
        var entity = OutboxEventEntity.newInstance();
        entity.setId(event.id());
        entity.setAggregateType(event.aggregateType());
        entity.setAggregateId(event.aggregateId());
        entity.setEventType(event.eventType());
        entity.setPayload(event.payload());
        entity.setStatus(event.status());
        entity.setCreatedAt(event.createdAt());
        entity.setProcessedAt(event.processedAt());
        return entity;
    }
}
