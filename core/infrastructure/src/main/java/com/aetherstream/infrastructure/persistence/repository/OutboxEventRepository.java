package com.aetherstream.infrastructure.persistence.repository;

import com.aetherstream.domain.outbox.OutboxStatus;
import com.aetherstream.infrastructure.persistence.entity.OutboxEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for the transactional outbox. The relay polls PENDING rows oldest-first;
 * locking/skip-locked semantics are added in the relay phase.
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Limit limit);
}
