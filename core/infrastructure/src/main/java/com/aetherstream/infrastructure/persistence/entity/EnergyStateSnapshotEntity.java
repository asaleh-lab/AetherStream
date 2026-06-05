package com.aetherstream.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Read-model: a point-in-time aggregated energy state for a region. */
@Entity
@Table(name = "energy_state_snapshot")
public class EnergyStateSnapshotEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "region", nullable = false)
    private String region;

    @Column(name = "total_wind_power", nullable = false)
    private double totalWindPower;

    @Column(name = "grid_demand", nullable = false)
    private double gridDemand;

    @Column(name = "efficiency_score", nullable = false)
    private double efficiencyScore;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    protected EnergyStateSnapshotEntity() {
        // JPA
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

    public double getTotalWindPower() {
        return totalWindPower;
    }

    public void setTotalWindPower(double totalWindPower) {
        this.totalWindPower = totalWindPower;
    }

    public double getGridDemand() {
        return gridDemand;
    }

    public void setGridDemand(double gridDemand) {
        this.gridDemand = gridDemand;
    }

    public double getEfficiencyScore() {
        return efficiencyScore;
    }

    public void setEfficiencyScore(double efficiencyScore) {
        this.efficiencyScore = efficiencyScore;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
