package com.aetherstream.application.port.out;

import com.aetherstream.domain.model.Turbine;

/** Outbound port for upserting the turbine write model. */
public interface TurbineStateStore {

    void upsert(Turbine turbine);
}
