package com.beachmonitor.cep.persistence.repository;

import com.beachmonitor.cep.persistence.entity.SensorReadingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReadingEntity, Long> {

    @Query(value = """
        SELECT * FROM beach_monitor.sensor_readings
        WHERE reading_time >= NOW() - (:seconds * INTERVAL '1 second')
        ORDER BY reading_time DESC
    """, nativeQuery = true)
    List<SensorReadingEntity> findLastSeconds(@Param("seconds") int seconds);

    @Query(value = """
        SELECT DISTINCT ON (beach_id, sensor_type)
            id, beach_id, sensor_type, value, unit, reading_time, created_at
        FROM beach_monitor.sensor_readings
        ORDER BY beach_id, sensor_type, reading_time DESC
    """, nativeQuery = true)
    List<SensorReadingEntity> findLatestPerBeachAndSensor();
}
