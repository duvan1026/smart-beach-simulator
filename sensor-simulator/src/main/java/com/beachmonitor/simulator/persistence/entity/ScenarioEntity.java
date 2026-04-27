package com.beachmonitor.simulator.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "scenarios", schema = "beach_monitor")
public class ScenarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_name", nullable = false)
    private String scenarioName;

    @Column(name = "sensor_type", nullable = false)
    private String sensorType;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    @Column(name = "variation_min")
    private Double variationMin;

    @Column(name = "variation_max")
    private Double variationMax;

    public String getScenarioName() { return scenarioName; }
    public String getSensorType() { return sensorType; }
    public Double getMinValue() { return minValue; }
    public Double getMaxValue() { return maxValue; }
    public Double getVariationMin() { return variationMin; }
    public Double getVariationMax() { return variationMax; }
}
