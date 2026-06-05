package com.aetherstream.application.query;

import com.aetherstream.application.cqrs.Query;
import com.aetherstream.domain.model.Turbine;
import java.util.Optional;

/** Returns the latest known state for a single turbine. */
public record GetTurbineQuery(String turbineId) implements Query<Optional<Turbine>> {}
