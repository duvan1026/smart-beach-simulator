package com.beachmonitor.cep.persistence.repository;

import com.beachmonitor.cep.persistence.entity.ComplexEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplexEventRepository extends JpaRepository<ComplexEventEntity, Long> {

    List<ComplexEventEntity> findTop20ByOrderByDetectedAtDesc();
}
