package com.beachmonitor.simulator.service.simulators;

import com.beachmonitor.simulator.config.SensorConfigLoader;
import com.beachmonitor.simulator.model.SensorReading;
import com.beachmonitor.simulator.utils.SensorBehaviorUtils;
import com.beachmonitor.simulator.utils.TimeUtils;

public class PressureSimulator {

    private double currentValue;
    private final String beachId;
    private final SensorConfigLoader config;

    public PressureSimulator(String beachId, SensorConfigLoader config) {
        this.beachId = beachId;
        this.config = config;
        this.currentValue = SensorBehaviorUtils.initialValue(
                config.getScenarioValue("NORMAL", "pressure", "min"),
                config.getScenarioValue("NORMAL", "pressure", "max"));
    }

    public SensorReading generate(String scenario) {
        double min = config.getScenarioValue(scenario, "pressure", "min");
        double max = config.getScenarioValue(scenario, "pressure", "max");
        double varMin = config.getScenarioValue(scenario, "pressure", "variationMin");
        double varMax = config.getScenarioValue(scenario, "pressure", "variationMax");

        currentValue = SensorBehaviorUtils.nextSensorValue(currentValue, varMin, varMax, min, max);

        return new SensorReading(beachId, "pressure", currentValue,
                config.getSensorUnits().get("pressure"), TimeUtils.nowIso());
    }
}
