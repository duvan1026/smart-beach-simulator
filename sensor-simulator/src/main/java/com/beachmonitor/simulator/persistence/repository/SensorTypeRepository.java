package com.beachmonitor.simulator.persistence.repository;

import com.beachmonitor.simulator.persistence.entity.SensorTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorTypeRepository extends JpaRepository<SensorTypeEntity, Long> {
}
