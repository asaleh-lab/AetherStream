package com.aetherstream.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Read-model: an optimization recommendation from the decision-engine stream. */
@Entity
@Table(name = "recommendations")
public class RecommendationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "region", nullable = false)
    private String region;

    @Column(name = "suggestion", nullable = false)
    private String suggestion;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    protected RecommendationEntity() {
        // JPA
    }

    public static RecommendationEntity newInstance() {
        return new RecommendationEntity();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
