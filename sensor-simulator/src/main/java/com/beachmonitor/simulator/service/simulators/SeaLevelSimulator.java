package com.beachmonitor.simulator.service.simulators;

import com.beachmonitor.simulator.config.SensorConfigLoader;
import com.beachmonitor.simulator.model.SensorReading;
import com.beachmonitor.simulator.utils.SensorBehaviorUtils;
import com.beachmonitor.simulator.utils.TimeUtils;

public class SeaLevelSimulator {

    private double currentValue;
    private final String beachId;
    private final SensorConfigLoader config;

    public SeaLevelSimulator(String beachId, SensorConfigLoader config) {
        this.beachId = beachId;
        this.config = config;
        this.currentValue = SensorBehaviorUtils.initialValue(
                config.getScenarioValue("NORMAL", "sealevel", "min"),
                config.getScenarioValue("NORMAL", "sealevel", "max"));
    }

    public SensorReading generate(String scenario) {
        double min = config.getScenarioValue(scenario, "sealevel", "min");
        double max = config.getScenarioValue(scenario, "sealevel", "max");
        double varMin = config.getScenarioValue(scenario, "sealevel", "variationMin");
        double varMax = config.getScenarioValue(scenario, "sealevel", "variationMax");

        currentValue = SensorBehaviorUtils.nextSensorValue(currentValue, varMin, varMax, min, max);

        return new SensorReading(beachId, "sealevel", currentValue,
                config.getSensorUnits().get("sealevel"), TimeUtils.nowIso());
    }
}
