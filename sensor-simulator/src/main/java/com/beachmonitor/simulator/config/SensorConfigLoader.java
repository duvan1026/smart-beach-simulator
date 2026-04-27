package com.beachmonitor.simulator.config;

import com.beachmonitor.simulator.persistence.entity.BeachEntity;
import com.beachmonitor.simulator.persistence.entity.ScenarioEntity;
import com.beachmonitor.simulator.persistence.entity.SensorTypeEntity;
import com.beachmonitor.simulator.persistence.repository.BeachRepository;
import com.beachmonitor.simulator.persistence.repository.ScenarioRepository;
import com.beachmonitor.simulator.persistence.repository.SensorTypeRepository;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SensorConfigLoader {

    private final BeachRepository beachRepository;
    private final SensorTypeRepository sensorTypeRepository;
    private final ScenarioRepository scenarioRepository;

    private List<String> beachIds = new ArrayList<>();
    private Map<String, String> sensorUnits = new HashMap<>();
    // scenario -> sensorType -> {min, max, variationMin, variationMax}
    private Map<String, Map<String, double[]>> scenarioParams = new HashMap<>();

    public SensorConfigLoader(BeachRepository beachRepository,
                              SensorTypeRepository sensorTypeRepository,
                              ScenarioRepository scenarioRepository) {
        this.beachRepository = beachRepository;
        this.sensorTypeRepository = sensorTypeRepository;
        this.scenarioRepository = scenarioRepository;
    }

    @PostConstruct
    public void load() {
        // Load beaches
        for (BeachEntity beach : beachRepository.findByEnabledTrue()) {
            beachIds.add(beach.getBeachId());
        }

        // Load sensor types and units
        for (SensorTypeEntity sensor : sensorTypeRepository.findAll()) {
            sensorUnits.put(sensor.getSensorType(), sensor.getUnit());
        }

        // Load all scenarios
        for (ScenarioEntity sc : scenarioRepository.findAll()) {
            scenarioParams
                    .computeIfAbsent(sc.getScenarioName(), k -> new HashMap<>())
                    .put(sc.getSensorType(), new double[]{
                            sc.getMinValue(), sc.getMaxValue(),
                            sc.getVariationMin(), sc.getVariationMax()
                    });
        }

        System.out.println("Sensor config loaded from DB: " + beachIds.size() + " beaches, "
                + sensorUnits.size() + " sensor types, " + scenarioParams.size() + " scenarios");
    }

    public List<String> getBeachIds() { return beachIds; }
    public Map<String, String> getSensorUnits() { return sensorUnits; }

    public double getScenarioValue(String scenario, String sensorType, String param) {
        double[] values = scenarioParams.get(scenario).get(sensorType);
        return switch (param) {
            case "min" -> values[0];
            case "max" -> values[1];
            case "variationMin" -> values[2];
            case "variationMax" -> values[3];
            default -> throw new IllegalArgumentException("Unknown param: " + param);
        };
    }

    public List<String> getSensorTypes() {
        return new ArrayList<>(sensorUnits.keySet());
    }
}
