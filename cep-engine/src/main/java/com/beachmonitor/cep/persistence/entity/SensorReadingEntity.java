package com.beachmonitor.cep.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_readings", schema = "beach_monitor")
public class SensorReadingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "beach_id", nullable = false)
    private String beachId;

    @Column(name = "sensor_type", nullable = false)
    private String sensorType;

    @Column(nullable = false)
    private Double value;

    @Column(nullable = false)
    private String unit;

    @Column(name = "reading_time", nullable = false)
    private LocalDateTime readingTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public SensorReadingEntity() {}

    public SensorReadingEntity(String beachId, String sensorType, Double value,
                               String unit, LocalDateTime readingTime) {
        this.beachId = beachId;
        this.sensorType = sensorType;
        this.value = value;
        this.unit = unit;
        this.readingTime = readingTime;
    }

    public Long getId() { return id; }
    public String getBeachId() { return beachId; }
    public String getSensorType() { return sensorType; }
    public Double getValue() { return value; }
    public String getUnit() { return unit; }
    public LocalDateTime getReadingTime() { return readingTime; }
}
