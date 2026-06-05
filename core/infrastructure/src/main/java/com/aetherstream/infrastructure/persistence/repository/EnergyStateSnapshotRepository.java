package com.aetherstream.infrastructure.persistence.repository;

import com.aetherstream.infrastructure.persistence.entity.EnergyStateSnapshotEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Read-model repository for aggregated energy-state snapshots. */
public interface EnergyStateSnapshotRepository
        extends JpaRepository<EnergyStateSnapshotEntity, UUID> {

    Optional<EnergyStateSnapshotEntity> findFirstByRegionOrderByTimestampDesc(String region);

    @Query(
            value =
                    """
                    SELECT DISTINCT ON (region) *
                    FROM energy_state_snapshot
                    ORDER BY region, timestamp DESC
                    """,
            nativeQuery = true)
    List<EnergyStateSnapshotEntity> findLatestPerRegion();
}
