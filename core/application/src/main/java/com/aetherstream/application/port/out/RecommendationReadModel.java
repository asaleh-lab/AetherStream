package com.aetherstream.application.port.out;

import com.aetherstream.domain.model.Recommendation;
import java.util.List;
import java.util.UUID;

/** Read-side port for optimization recommendations projected from Kafka. */
public interface RecommendationReadModel {

    void upsert(UUID eventId, Recommendation recommendation);

    List<Recommendation> findRecent(int limit);
}
