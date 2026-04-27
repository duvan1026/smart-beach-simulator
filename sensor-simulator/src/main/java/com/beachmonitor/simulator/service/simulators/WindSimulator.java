package com.beachmonitor.simulator.service.simulators;

import com.beachmonitor.simulator.config.SensorConfigLoader;
import com.beachmonitor.simulator.model.SensorReading;
import com.beachmonitor.simulator.utils.SensorBehaviorUtils;
import com.beachmonitor.simulator.utils.TimeUtils;

public class WindSimulator {

    private double currentValue;
    private final String beachId;
    private final SensorConfigLoader config;

    public WindSimulator(String beachId, SensorConfigLoader config) {
        this.beachId = beachId;
        this.config = config;
        this.currentValue = SensorBehaviorUtils.initialValue(
                config.getScenarioValue("NORMAL", "wind", "min"),
                config.getScenarioValue("NORMAL", "wind", "max"));
    }

    public SensorReading generate(String scenario) {
        double min = config.getScenarioValue(scenario, "wind", "min");
        double max = config.getScenarioValue(scenario, "wind", "max");
        double varMin = config.getScenarioValue(scenario, "wind", "variationMin");
        double varMax = config.getScenarioValue(scenario, "wind", "variationMax");

        currentValue = SensorBehaviorUtils.nextSensorValue(currentValue, varMin, varMax, min, max);

        return new SensorReading(beachId, "wind", currentValue,
                config.getSensorUnits().get("wind"), TimeUtils.nowIso());
    }
}
