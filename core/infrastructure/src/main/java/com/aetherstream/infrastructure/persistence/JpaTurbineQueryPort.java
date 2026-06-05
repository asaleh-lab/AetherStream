package com.aetherstream.infrastructure.persistence;

import com.aetherstream.application.port.out.TurbineQueryPort;
import com.aetherstream.domain.model.Turbine;
import com.aetherstream.infrastructure.persistence.repository.TurbineStateRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaTurbineQueryPort implements TurbineQueryPort {

    private final TurbineStateRepository repository;

    public JpaTurbineQueryPort(TurbineStateRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Turbine> findById(String turbineId) {
        return repository.findById(turbineId).map(entity -> new Turbine(
                entity.getTurbineId(),
                entity.getRpm(),
                entity.getPowerOutput(),
                entity.getVibrationLevel(),
                entity.getUpdatedAt()));
    }
}
