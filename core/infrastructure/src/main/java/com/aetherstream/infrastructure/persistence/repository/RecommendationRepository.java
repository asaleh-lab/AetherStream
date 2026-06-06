package com.aetherstream.infrastructure.persistence.repository;

import com.aetherstream.infrastructure.persistence.entity.RecommendationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

/** Read-model repository for optimization recommendations. */
public interface RecommendationRepository extends JpaRepository<RecommendationEntity, UUID> {

    List<RecommendationEntity> findByOrderByTimestampDesc(Limit limit);
}
