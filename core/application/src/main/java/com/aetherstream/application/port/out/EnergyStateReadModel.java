package com.aetherstream.application.port.out;

import com.aetherstream.domain.model.EnergyState;
import java.util.List;
import java.util.UUID;

/** Outbound port for the energy-state read-model projection. */
public interface EnergyStateReadModel {

    void upsert(UUID eventId, EnergyState state);

    List<EnergyState> findLatestPerRegion();
}
