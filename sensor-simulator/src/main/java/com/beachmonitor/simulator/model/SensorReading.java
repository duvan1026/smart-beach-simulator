package com.beachmonitor.simulator.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class SensorReading {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String beachId;
    private String sensorType;
    private double value;
    private String unit;
    private String timestamp;

    public SensorReading() {}

    public SensorReading(String beachId, String sensorType, double value,
                         String unit, String timestamp) {
        this.beachId = beachId;
        this.sensorType = sensorType;
        this.value = value;
        this.unit = unit;
        this.timestamp = timestamp;
    }

    public String toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("beachId", beachId);
        map.put("sensorType", sensorType);
        map.put("value", value);
        map.put("unit", unit);
        map.put("timestamp", timestamp);
        try {
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing SensorReading", e);
        }
    }

    public String getBeachId() { return beachId; }
    public void setBeachId(String beachId) { this.beachId = beachId; }
    public String getSensorType() { return sensorType; }
    public void setSensorType(String sensorType) { this.sensorType = sensorType; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
