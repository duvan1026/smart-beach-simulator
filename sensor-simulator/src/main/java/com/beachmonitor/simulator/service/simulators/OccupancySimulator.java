package com.beachmonitor.simulator.service.simulators;

import com.beachmonitor.simulator.config.SensorConfigLoader;
import com.beachmonitor.simulator.model.OccupancyParams;
import com.beachmonitor.simulator.model.SensorReading;
import com.beachmonitor.simulator.utils.NumberUtils;
import com.beachmonitor.simulator.utils.RandomUtils;
import com.beachmonitor.simulator.utils.SensorBehaviorUtils;
import com.beachmonitor.simulator.utils.TimeUtils;

public class OccupancySimulator {

    private double currentValue;
    private final String beachId;
    private final SensorConfigLoader config;

    public OccupancySimulator(String beachId, SensorConfigLoader config) {
        this.beachId = beachId;
        this.config = config;
        this.currentValue = SensorBehaviorUtils.initialValue(
                config.getScenarioValue("NORMAL", "occupancy", "min"),
                config.getScenarioValue("NORMAL", "occupancy", "max"));
    }

    /**
     * Generate occupancy based on dashboard parameters (hour, temperature, day of week).
     * The base occupancy is calculated from those factors, then we add small random variation
     * to simulate real fluctuation, and clamp to the scenario min/max bounds.
     */
    public SensorReading generate(String scenario, OccupancyParams params) {
        double scenarioMin = config.getScenarioValue(scenario, "occupancy", "min");
        double scenarioMax = config.getScenarioValue(scenario, "occupancy", "max");
        double varMin = config.getScenarioValue(scenario, "occupancy", "variationMin");
        double varMax = config.getScenarioValue(scenario, "occupancy", "variationMax");

        if (params != null) {
            // Calculate base from hour + temperature + day of week
            double base = params.calculateBaseOccupancy();

            // Blend with scenario bounds: the scenario can limit or boost occupancy
            // e.g., STORM forces low occupancy, HIGH_OCCUPANCY forces high
            double target = Math.max(scenarioMin, Math.min(scenarioMax, base));

            // Smooth transition towards target (don't jump instantly)
            double diff = target - currentValue;
            double step = diff * 0.3 + RandomUtils.randomInRange(-varMax, varMax);
            currentValue = NumberUtils.roundToTwoDecimals(currentValue + step);
        } else {
            // Fallback: simple random walk within scenario bounds
            currentValue = SensorBehaviorUtils.nextSensorValue(currentValue, varMin, varMax, scenarioMin, scenarioMax);
        }

        // Clamp to 0-100 integer
        int intValue = (int) Math.round(currentValue);
        intValue = Math.max(0, Math.min(100, intValue));
        currentValue = intValue;

        return new SensorReading(beachId, "occupancy", intValue,
                config.getSensorUnits().get("occupancy"), TimeUtils.nowIso());
    }
}
