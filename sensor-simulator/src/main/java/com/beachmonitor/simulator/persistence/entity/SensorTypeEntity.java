package com.beachmonitor.simulator.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sensor_types", schema = "beach_monitor")
public class SensorTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_type", nullable = false, unique = true)
    private String sensorType;

    private String unit;

    @Column(name = "normal_min")
    private Double normalMin;

    @Column(name = "normal_max")
    private Double normalMax;

    @Column(name = "variation_min")
    private Double variationMin;

    @Column(name = "variation_max")
    private Double variationMax;

    public String getSensorType() { return sensorType; }
    public String getUnit() { return unit; }
    public Double getNormalMin() { return normalMin; }
    public Double getNormalMax() { return normalMax; }
    public Double getVariationMin() { return variationMin; }
    public Double getVariationMax() { return variationMax; }
}
