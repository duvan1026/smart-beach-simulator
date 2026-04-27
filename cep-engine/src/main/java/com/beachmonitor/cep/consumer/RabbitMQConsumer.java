package com.beachmonitor.cep.consumer;

import com.beachmonitor.cep.engine.EsperEventProcessor;
import com.beachmonitor.cep.persistence.service.PersistenceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RabbitMQConsumer {

    private final EsperEventProcessor esperProcessor;
    private final PersistenceService persistenceService;
    private final com.beachmonitor.cep.websocket.SensorWebSocketHandler webSocketHandler;
    private final ObjectMapper mapper = new ObjectMapper();

    public RabbitMQConsumer(EsperEventProcessor esperProcessor,
                            PersistenceService persistenceService,
                            com.beachmonitor.cep.websocket.SensorWebSocketHandler webSocketHandler) {
        this.esperProcessor = esperProcessor;
        this.persistenceService = persistenceService;
        this.webSocketHandler = webSocketHandler;
    }

    @RabbitListener(queues = "beach.sensors")
    public void onMessage(Message message) {
        try {
            String body = new String(message.getBody());
            Map<String, Object> raw = mapper.readValue(body, new TypeReference<>() {});

            // Build the BeachCombinedEvent map for Esper
            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("beachId", raw.get("beachId"));
            eventMap.put("timestamp", raw.get("timestamp"));

            // Handle numeric conversions carefully
            eventMap.put("windSpeed", toDouble(raw.get("windSpeed")));
            eventMap.put("pressure", toDouble(raw.get("pressure")));
            eventMap.put("temperature", toDouble(raw.get("temperature")));
            eventMap.put("seaLevel", toDouble(raw.get("seaLevel")));
            eventMap.put("occupancy", toInt(raw.get("occupancy")));

            // 1. Persist simple sensor readings to PostgreSQL
            persistenceService.saveSensorReadings(eventMap);

            // 2. Send combined event to Esper CEP for pattern matching
            esperProcessor.sendBeachCombinedEvent(eventMap);

            // 3. Broadcast readings to dashboard via WebSocket
            webSocketHandler.broadcastReadings(eventMap);

        } catch (Exception e) {
            System.err.println("Error processing RabbitMQ message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val instanceof String) return Double.parseDouble((String) val);
        return 0.0;
    }

    private int toInt(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) return Integer.parseInt((String) val);
        return 0;
    }
}
