package com.beachmonitor.simulator.service;

import com.beachmonitor.simulator.config.SensorConfigLoader;
import com.beachmonitor.simulator.model.OccupancyParams;
import com.beachmonitor.simulator.model.SensorReading;
import com.beachmonitor.simulator.model.WeatherScenario;
import com.beachmonitor.simulator.service.simulators.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class SensorSimulatorJob {

    private final MqttPublisherService mqttPublisher;
    private final SensorConfigLoader config;

    private volatile WeatherScenario currentScenario = WeatherScenario.NORMAL;
    private volatile OccupancyParams occupancyParams = new OccupancyParams();

    // beachId -> simulators
    private final Map<String, WindSimulator> windSimulators = new HashMap<>();
    private final Map<String, PressureSimulator> pressureSimulators = new HashMap<>();
    private final Map<String, TemperatureSimulator> temperatureSimulators = new HashMap<>();
    private final Map<String, SeaLevelSimulator> seaLevelSimulators = new HashMap<>();
    private final Map<String, OccupancySimulator> occupancySimulators = new HashMap<>();

    public SensorSimulatorJob(MqttPublisherService mqttPublisher, SensorConfigLoader config) {
        this.mqttPublisher = mqttPublisher;
        this.config = config;
    }

    @PostConstruct
    public void init() {
        for (String beachId : config.getBeachIds()) {
            windSimulators.put(beachId, new WindSimulator(beachId, config));
            pressureSimulators.put(beachId, new PressureSimulator(beachId, config));
            temperatureSimulators.put(beachId, new TemperatureSimulator(beachId, config));
            seaLevelSimulators.put(beachId, new SeaLevelSimulator(beachId, config));
            occupancySimulators.put(beachId, new OccupancySimulator(beachId, config));
        }
        System.out.println("Sensor simulators initialized for beaches: " + config.getBeachIds());
    }

    @Scheduled(fixedRate = 3000)
    public void sendSensorData() {
        String scenario = currentScenario.name();

        for (String beachId : config.getBeachIds()) {
            // Generate readings for all 5 sensor types
            SensorReading wind = windSimulators.get(beachId).generate(scenario);
            SensorReading pressure = pressureSimulators.get(beachId).generate(scenario);
            SensorReading temperature = temperatureSimulators.get(beachId).generate(scenario);
            SensorReading seaLevel = seaLevelSimulators.get(beachId).generate(scenario);
            SensorReading occupancy = occupancySimulators.get(beachId).generate(scenario, occupancyParams);

            // Publish each to its MQTT topic: beach/{beachId}/{sensorType}
            mqttPublisher.publish(wind.toJson(), "beach/" + beachId + "/wind");
            mqttPublisher.publish(pressure.toJson(), "beach/" + beachId + "/pressure");
            mqttPublisher.publish(temperature.toJson(), "beach/" + beachId + "/temperature");
            mqttPublisher.publish(seaLevel.toJson(), "beach/" + beachId + "/sealevel");
            mqttPublisher.publish(occupancy.toJson(), "beach/" + beachId + "/occupancy");
        }
    }

    public void setScenario(WeatherScenario scenario) {
        this.currentScenario = scenario;
        System.out.println("Scenario changed to: " + scenario);
    }

    public WeatherScenario getCurrentScenario() {
        return currentScenario;
    }

    public void setOccupancyParams(OccupancyParams params) {
        this.occupancyParams = params;
        System.out.println("Occupancy params updated: hour=" + params.getHour()
                + ", day=" + params.getDayOfWeek());
    }

    public OccupancyParams getOccupancyParams() {
        return occupancyParams;
    }
}
