package com.beachmonitor.simulator.service.simulators;

import com.beachmonitor.simulator.config.SensorConfigLoader;
import com.beachmonitor.simulator.model.SensorReading;
import com.beachmonitor.simulator.utils.SensorBehaviorUtils;
import com.beachmonitor.simulator.utils.TimeUtils;

public class TemperatureSimulator {

    private double currentValue;
    private final String beachId;
    private final SensorConfigLoader config;

    public TemperatureSimulator(String beachId, SensorConfigLoader config) {
        this.beachId = beachId;
        this.config = config;
        this.currentValue = SensorBehaviorUtils.initialValue(
                config.getScenarioValue("NORMAL", "temperature", "min"),
                config.getScenarioValue("NORMAL", "temperature", "max"));
    }

    public SensorReading generate(String scenario) {
        double min = config.getScenarioValue(scenario, "temperature", "min");
        double max = config.getScenarioValue(scenario, "temperature", "max");
        double varMin = config.getScenarioValue(scenario, "temperature", "variationMin");
        double varMax = config.getScenarioValue(scenario, "temperature", "variationMax");

        currentValue = SensorBehaviorUtils.nextSensorValue(currentValue, varMin, varMax, min, max);

        return new SensorReading(beachId, "temperature", currentValue,
                config.getSensorUnits().get("temperature"), TimeUtils.nowIso());
    }
}
