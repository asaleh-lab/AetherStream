package com.aetherstream.application.query;

import com.aetherstream.application.cqrs.Query;
import com.aetherstream.domain.model.Recommendation;
import java.util.List;

/** Returns recent recommendations ordered newest-first. */
public record GetRecommendationsQuery(int limit) implements Query<List<Recommendation>> {

    public GetRecommendationsQuery() {
        this(50);
    }
}
