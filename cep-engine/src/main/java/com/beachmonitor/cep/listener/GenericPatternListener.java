package com.beachmonitor.cep.listener;

import com.beachmonitor.cep.persistence.service.PersistenceService;
import com.beachmonitor.cep.publisher.MqttAlertPublisher;
import com.beachmonitor.cep.websocket.SensorWebSocketHandler;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

import java.util.LinkedHashMap;
import java.util.Map;

public class GenericPatternListener implements UpdateListener {

    private final String patternId;
    private final String patternName;
    private final String alertLevel;
    private final PersistenceService persistenceService;
    private final MqttAlertPublisher alertPublisher;
    private final SensorWebSocketHandler webSocketHandler;

    public GenericPatternListener(String patternId, String patternName, String alertLevel,
                                  PersistenceService persistenceService,
                                  MqttAlertPublisher alertPublisher,
                                  SensorWebSocketHandler webSocketHandler) {
        this.patternId = patternId;
        this.patternName = patternName;
        this.alertLevel = alertLevel;
        this.persistenceService = persistenceService;
        this.alertPublisher = alertPublisher;
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void update(EventBean[] newEvents, EventBean[] oldEvents,
                       EPStatement statement, EPRuntime runtime) {
        if (newEvents == null) return;

        for (EventBean event : newEvents) {
            try {
                Map<String, Object> details = new LinkedHashMap<>();
                for (String prop : event.getEventType().getPropertyNames()) {
                    details.put(prop, event.get(prop));
                }

                String beachId = extractBeachId(details);
                String message = buildMessage(details);

                // Convert details to JSON string for DB storage
                String detailsJson = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(details);

                // Publish alert to MQTT
                alertPublisher.publishAlert(patternId, patternName, alertLevel,
                        beachId, details, message);

                // Broadcast alert to dashboard via WebSocket
                webSocketHandler.broadcastAlert(patternId, patternName, alertLevel,
                        beachId, details, message);

                // Persist complex event to PostgreSQL
                persistenceService.saveComplexEvent(patternId, patternName, alertLevel,
                        beachId, detailsJson, message);

            } catch (Exception e) {
                System.err.println("Error processing pattern " + patternId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private String extractBeachId(Map<String, Object> details) {
        if (details.containsKey("beachId")) return String.valueOf(details.get("beachId"));
        if (details.containsKey("beach1")) return String.valueOf(details.get("beach1"));
        return "unknown";
    }

    private String buildMessage(Map<String, Object> details) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(alertLevel).append("] ").append(patternName);

        String beachId = extractBeachId(details);
        if (!"unknown".equals(beachId)) {
            sb.append(" at ").append(beachId);
        }

        sb.append(" -");
        if (details.containsKey("windSpeed"))
            sb.append(" Wind: ").append(formatNum(details.get("windSpeed"))).append(" km/h,");
        if (details.containsKey("pressure"))
            sb.append(" Pressure: ").append(formatNum(details.get("pressure"))).append(" hPa,");
        if (details.containsKey("currentPressure"))
            sb.append(" Current: ").append(formatNum(details.get("currentPressure"))).append(" hPa,");
        if (details.containsKey("temperature"))
            sb.append(" Temp: ").append(formatNum(details.get("temperature"))).append(" C,");
        if (details.containsKey("avgTemp"))
            sb.append(" AvgTemp: ").append(formatNum(details.get("avgTemp"))).append(" C,");
        if (details.containsKey("seaLevel"))
            sb.append(" Sea: ").append(formatNum(details.get("seaLevel"))).append(" m,");
        if (details.containsKey("occupancy"))
            sb.append(" Occupancy: ").append(details.get("occupancy")).append("%,");
        if (details.containsKey("avgWind"))
            sb.append(" AvgWind: ").append(formatNum(details.get("avgWind"))).append(" km/h,");

        // Remove trailing comma
        String result = sb.toString();
        if (result.endsWith(",")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private String formatNum(Object val) {
        if (val instanceof Double) return String.format("%.1f", (Double) val);
        return String.valueOf(val);
    }
}
