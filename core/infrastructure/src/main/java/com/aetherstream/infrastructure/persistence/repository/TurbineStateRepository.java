package com.aetherstream.infrastructure.persistence.repository;

import com.aetherstream.infrastructure.persistence.entity.TurbineStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Write-model repository for turbine state. */
public interface TurbineStateRepository extends JpaRepository<TurbineStateEntity, String> {
}
