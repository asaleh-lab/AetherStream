package com.aetherstream.infrastructure.persistence.repository;

import com.aetherstream.infrastructure.persistence.entity.EnergyStateSnapshotEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Read-model repository for aggregated energy-state snapshots. */
public interface EnergyStateSnapshotRepository
        extends JpaRepository<EnergyStateSnapshotEntity, UUID> {

    Optional<EnergyStateSnapshotEntity> findFirstByRegionOrderByTimestampDesc(String region);
}
