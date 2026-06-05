package com.aetherstream.infrastructure.persistence.repository;

import com.aetherstream.infrastructure.persistence.entity.AlertEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

/** Read-model repository for alerts. */
public interface AlertRepository extends JpaRepository<AlertEntity, UUID> {

    List<AlertEntity> findByOrderByTimestampDesc(Limit limit);
}
