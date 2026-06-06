package com.aetherstream.infrastructure.persistence;

import com.aetherstream.application.port.out.RecommendationReadModel;
import com.aetherstream.domain.model.Recommendation;
import com.aetherstream.infrastructure.persistence.entity.RecommendationEntity;
import com.aetherstream.infrastructure.persistence.repository.RecommendationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaRecommendationReadModel implements RecommendationReadModel {

    private final RecommendationRepository repository;

    public JpaRecommendationReadModel(RecommendationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void upsert(UUID eventId, Recommendation recommendation) {
        if (repository.existsById(eventId)) {
            return;
        }
        var entity = RecommendationEntity.newInstance();
        entity.setId(eventId);
        entity.setRegion(recommendation.region());
        entity.setSuggestion(recommendation.suggestion());
        entity.setTimestamp(recommendation.timestamp());
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recommendation> findRecent(int limit) {
        return repository.findByOrderByTimestampDesc(Limit.of(limit)).stream()
                .map(JpaRecommendationReadModel::toDomain)
                .toList();
    }

    private static Recommendation toDomain(RecommendationEntity entity) {
        return new Recommendation(
                entity.getId().toString(),
                entity.getRegion(),
                entity.getSuggestion(),
                entity.getTimestamp());
    }
}
