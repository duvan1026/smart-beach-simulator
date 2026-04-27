package com.beachmonitor.cep.persistence.repository;

import com.beachmonitor.cep.persistence.entity.EsperPatternEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EsperPatternRepository extends JpaRepository<EsperPatternEntity, Long> {

    List<EsperPatternEntity> findByEnabledTrue();
}
