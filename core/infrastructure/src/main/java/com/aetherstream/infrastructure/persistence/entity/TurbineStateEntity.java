package com.aetherstream.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Write-model: latest known state for a turbine. */
@Entity
@Table(name = "turbine_state")
public class TurbineStateEntity {

    @Id
    @Column(name = "turbine_id", nullable = false, updatable = false)
    private String turbineId;

    @Column(name = "rpm", nullable = false)
    private double rpm;

    @Column(name = "power_output", nullable = false)
    private double powerOutput;

    @Column(name = "vibration_level", nullable = false)
    private double vibrationLevel;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TurbineStateEntity() {
        // JPA
    }

    public String getTurbineId() {
        return turbineId;
    }

    public void setTurbineId(String turbineId) {
        this.turbineId = turbineId;
    }

    public double getRpm() {
        return rpm;
    }

    public void setRpm(double rpm) {
        this.rpm = rpm;
    }

    public double getPowerOutput() {
        return powerOutput;
    }

    public void setPowerOutput(double powerOutput) {
        this.powerOutput = powerOutput;
    }

    public double getVibrationLevel() {
        return vibrationLevel;
    }

    public void setVibrationLevel(double vibrationLevel) {
        this.vibrationLevel = vibrationLevel;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
