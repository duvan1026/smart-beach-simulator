package com.beachmonitor.simulator.persistence.repository;

import com.beachmonitor.simulator.persistence.entity.BeachEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeachRepository extends JpaRepository<BeachEntity, Long> {
    List<BeachEntity> findByEnabledTrue();
}
