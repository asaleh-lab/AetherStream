package com.aetherstream.infrastructure.persistence.repository;

import com.aetherstream.domain.outbox.OutboxStatus;
import com.aetherstream.infrastructure.persistence.entity.OutboxEventEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for the transactional outbox. The relay polls PENDING rows oldest-first
 * using {@code FOR UPDATE SKIP LOCKED} for safe concurrent polling.
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(
            value =
                    """
                    SELECT * FROM outbox_events
                    WHERE status = 'PENDING'
                    ORDER BY created_at ASC
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true)
    List<OutboxEventEntity> lockPendingBatch(@Param("limit") int limit);

    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Limit limit);

    Optional<OutboxEventEntity> findFirstByAggregateIdAndStatusOrderByCreatedAtDesc(
            String aggregateId, OutboxStatus status);
}
