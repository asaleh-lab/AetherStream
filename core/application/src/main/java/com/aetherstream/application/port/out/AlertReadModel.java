package com.aetherstream.application.port.out;

import com.aetherstream.domain.model.Alert;
import java.util.List;
import java.util.UUID;

/** Outbound port for the alerts read-model projection. */
public interface AlertReadModel {

    void upsert(UUID eventId, Alert alert);

    List<Alert> findRecent(int limit);
}
