package com.beachmonitor.cep.engine;

import com.espertech.esper.runtime.client.EPRuntime;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EsperEventProcessor {

    private final EPRuntime runtime;

    public EsperEventProcessor(EPRuntime runtime) {
        this.runtime = runtime;
    }

    public void sendBeachCombinedEvent(Map<String, Object> eventMap) {
        runtime.getEventService().sendEventMap(eventMap, "BeachCombinedEvent");
    }

    public void sendSensorEvent(Map<String, Object> eventMap) {
        runtime.getEventService().sendEventMap(eventMap, "SensorEvent");
    }
}
