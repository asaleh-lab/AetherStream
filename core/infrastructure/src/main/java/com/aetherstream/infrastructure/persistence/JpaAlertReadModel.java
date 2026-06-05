package com.aetherstream.infrastructure.persistence;

import com.aetherstream.application.port.out.AlertReadModel;
import com.aetherstream.domain.model.Alert;
import com.aetherstream.infrastructure.persistence.entity.AlertEntity;
import com.aetherstream.infrastructure.persistence.repository.AlertRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaAlertReadModel implements AlertReadModel {

    private final AlertRepository repository;

    public JpaAlertReadModel(AlertRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void upsert(UUID eventId, Alert alert) {
        if (repository.existsById(eventId)) {
            return;
        }
        var entity = AlertEntity.newInstance();
        entity.setId(eventId);
        entity.setType(alert.type());
        entity.setSeverity(alert.severity());
        entity.setSource(alert.source());
        entity.setMessage(alert.message());
        entity.setTimestamp(alert.timestamp());
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> findRecent(int limit) {
        return repository.findByOrderByTimestampDesc(Limit.of(limit)).stream()
                .map(JpaAlertReadModel::toDomain)
                .toList();
    }

    private static Alert toDomain(AlertEntity entity) {
        return new Alert(
                entity.getId().toString(),
                entity.getType(),
                entity.getSeverity(),
                entity.getSource(),
                entity.getMessage(),
                entity.getTimestamp());
    }
}
