package com.beachmonitor.simulator.persistence.repository;

import com.beachmonitor.simulator.persistence.entity.ScenarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScenarioRepository extends JpaRepository<ScenarioEntity, Long> {
    List<ScenarioEntity> findByScenarioName(String scenarioName);
    Optional<ScenarioEntity> findByScenarioNameAndSensorType(String scenarioName, String sensorType);
}
