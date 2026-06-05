package com.aetherstream.application.query;

import com.aetherstream.application.cqrs.Query;
import com.aetherstream.domain.model.EnergyState;
import java.util.List;

/** Returns the latest aggregated energy state for each known region. */
public record GetLatestEnergyStatesQuery() implements Query<List<EnergyState>> {}
