package com.aetherstream.infrastructure.persistence;

import com.aetherstream.application.port.out.TurbineStateStore;
import com.aetherstream.domain.model.Turbine;
import com.aetherstream.infrastructure.persistence.entity.TurbineStateEntity;
import com.aetherstream.infrastructure.persistence.repository.TurbineStateRepository;
import org.springframework.stereotype.Service;

@Service
public class JpaTurbineStateStore implements TurbineStateStore {

    private final TurbineStateRepository repository;

    public JpaTurbineStateStore(TurbineStateRepository repository) {
        this.repository = repository;
    }

    @Override
    public void upsert(Turbine turbine) {
        var entity = repository.findById(turbine.turbineId()).orElseGet(TurbineStateEntity::newInstance);
        entity.setTurbineId(turbine.turbineId());
        entity.setRpm(turbine.rpm());
        entity.setPowerOutput(turbine.powerOutput());
        entity.setVibrationLevel(turbine.vibrationLevel());
        entity.setUpdatedAt(turbine.timestamp());
        repository.save(entity);
    }
}
