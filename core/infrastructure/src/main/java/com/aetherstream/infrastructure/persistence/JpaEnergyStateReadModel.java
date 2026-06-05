package com.aetherstream.infrastructure.persistence;

import com.aetherstream.application.port.out.EnergyStateReadModel;
import com.aetherstream.domain.model.EnergyState;
import com.aetherstream.infrastructure.persistence.entity.EnergyStateSnapshotEntity;
import com.aetherstream.infrastructure.persistence.repository.EnergyStateSnapshotRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaEnergyStateReadModel implements EnergyStateReadModel {

    private final EnergyStateSnapshotRepository repository;

    public JpaEnergyStateReadModel(EnergyStateSnapshotRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void upsert(UUID eventId, EnergyState state) {
        if (repository.existsById(eventId)) {
            return;
        }
        var entity = EnergyStateSnapshotEntity.newInstance();
        entity.setId(eventId);
        entity.setRegion(state.region());
        entity.setTotalWindPower(state.totalWindPower());
        entity.setGridDemand(state.gridDemand());
        entity.setEfficiencyScore(state.efficiencyScore());
        entity.setTimestamp(state.timestamp());
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnergyState> findLatestPerRegion() {
        return repository.findLatestPerRegion().stream()
                .map(JpaEnergyStateReadModel::toDomain)
                .toList();
    }

    private static EnergyState toDomain(EnergyStateSnapshotEntity entity) {
        return new EnergyState(
                entity.getRegion(),
                entity.getTotalWindPower(),
                entity.getGridDemand(),
                entity.getEfficiencyScore(),
                entity.getTimestamp());
    }
}
