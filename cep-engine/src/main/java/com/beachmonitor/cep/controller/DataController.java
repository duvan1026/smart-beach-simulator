package com.beachmonitor.cep.controller;

import com.beachmonitor.cep.persistence.entity.ComplexEventEntity;
import com.beachmonitor.cep.persistence.entity.SensorReadingEntity;
import com.beachmonitor.cep.persistence.repository.ComplexEventRepository;
import com.beachmonitor.cep.persistence.repository.SensorReadingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DataController {

    private final SensorReadingRepository sensorReadingRepository;
    private final ComplexEventRepository complexEventRepository;

    public DataController(SensorReadingRepository sensorReadingRepository,
                          ComplexEventRepository complexEventRepository) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.complexEventRepository = complexEventRepository;
    }

    /**
     * GET /api/readings/latest
     * Returns the latest reading per beach per sensor type.
     * Used by the dashboard to populate charts.
     */
    @GetMapping("/readings/latest")
    public ResponseEntity<List<Map<String, Object>>> getLatestReadings() {
        List<SensorReadingEntity> readings = sensorReadingRepository.findLatestPerBeachAndSensor();
        List<Map<String, Object>> result = new ArrayList<>();
        for (SensorReadingEntity r : readings) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("beachId", r.getBeachId());
            map.put("sensorType", r.getSensorType());
            map.put("value", r.getValue());
            map.put("unit", r.getUnit());
            map.put("timestamp", r.getReadingTime().toString());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/readings/history/{seconds}
     * Returns sensor readings from the last N seconds.
     */
    @GetMapping("/readings/history/{seconds}")
    public ResponseEntity<List<Map<String, Object>>> getReadingsHistory(@PathVariable int seconds) {
        List<SensorReadingEntity> readings = sensorReadingRepository.findLastSeconds(seconds);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SensorReadingEntity r : readings) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("beachId", r.getBeachId());
            map.put("sensorType", r.getSensorType());
            map.put("value", r.getValue());
            map.put("unit", r.getUnit());
            map.put("timestamp", r.getReadingTime().toString());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/events/latest
     * Returns the last 50 complex events detected by CEP.
     */
    @GetMapping("/events/latest")
    public ResponseEntity<List<Map<String, Object>>> getLatestEvents() {
        List<ComplexEventEntity> events = complexEventRepository.findTop20ByOrderByDetectedAtDesc();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ComplexEventEntity e : events) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("alertLevel", e.getAlertLevel());
            map.put("patternName", e.getPatternName());
            map.put("beachId", e.getBeachId());
            map.put("message", e.getMessage());
            map.put("timestamp", e.getDetectedAt().toString());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }
}
