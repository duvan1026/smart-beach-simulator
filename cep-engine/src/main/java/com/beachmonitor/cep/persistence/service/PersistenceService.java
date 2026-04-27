package com.beachmonitor.cep.persistence.service;

import com.beachmonitor.cep.persistence.entity.ComplexEventEntity;
import com.beachmonitor.cep.persistence.entity.SensorReadingEntity;
import com.beachmonitor.cep.persistence.repository.ComplexEventRepository;
import com.beachmonitor.cep.persistence.repository.SensorReadingRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Service
public class PersistenceService {

    private final SensorReadingRepository sensorReadingRepository;
    private final ComplexEventRepository complexEventRepository;

    public PersistenceService(SensorReadingRepository sensorReadingRepository,
                              ComplexEventRepository complexEventRepository) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.complexEventRepository = complexEventRepository;
    }

    public void saveSensorReadings(Map<String, Object> combinedEvent) {
        String beachId = (String) combinedEvent.get("beachId");
        String timestamp = (String) combinedEvent.get("timestamp");
        LocalDateTime readingTime = parseTimestamp(timestamp);

        saveSingleReading(beachId, "wind", (Double) combinedEvent.get("windSpeed"), "km/h", readingTime);
        saveSingleReading(beachId, "pressure", (Double) combinedEvent.get("pressure"), "hPa", readingTime);
        saveSingleReading(beachId, "temperature", (Double) combinedEvent.get("temperature"), "C", readingTime);
        saveSingleReading(beachId, "sealevel", (Double) combinedEvent.get("seaLevel"), "m", readingTime);

        Object occVal = combinedEvent.get("occupancy");
        double occupancy = (occVal instanceof Integer) ? ((Integer) occVal).doubleValue() : (Double) occVal;
        saveSingleReading(beachId, "occupancy", occupancy, "%", readingTime);
    }

    private void saveSingleReading(String beachId, String sensorType, Double value,
                                   String unit, LocalDateTime readingTime) {
        if (value == null) return;
        SensorReadingEntity entity = new SensorReadingEntity(beachId, sensorType, value, unit, readingTime);
        sensorReadingRepository.save(entity);
    }

    public ComplexEventEntity saveComplexEvent(String patternId, String patternName,
                                                String alertLevel, String beachId,
                                                String details, String message) {
        ComplexEventEntity entity = new ComplexEventEntity(patternId, patternName, alertLevel, beachId, details, message);
        return complexEventRepository.save(entity);
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            Instant instant = Instant.parse(timestamp);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
