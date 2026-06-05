package com.aetherstream.application.query;

import com.aetherstream.application.cqrs.Query;
import com.aetherstream.domain.model.Alert;
import java.util.List;

/** Returns recent alerts ordered newest-first. */
public record GetAlertsQuery(int limit) implements Query<List<Alert>> {

    public GetAlertsQuery() {
        this(50);
    }
}
