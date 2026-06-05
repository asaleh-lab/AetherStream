package com.aetherstream.application.port.out;

import com.aetherstream.domain.model.Turbine;
import java.util.Optional;

/** Outbound port for read-side turbine lookups against the write model. */
public interface TurbineQueryPort {

    Optional<Turbine> findById(String turbineId);
}
