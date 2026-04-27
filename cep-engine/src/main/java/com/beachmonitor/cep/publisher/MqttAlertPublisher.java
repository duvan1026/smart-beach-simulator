package com.beachmonitor.cep.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MqttAlertPublisher {

    private final MqttClient mqttClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public MqttAlertPublisher(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    public void publishAlert(String patternId, String patternName, String alertLevel,
                             String beachId, Map<String, Object> details, String message) {
        try {
            Map<String, Object> alertPayload = new LinkedHashMap<>();
            alertPayload.put("patternId", patternId);
            alertPayload.put("patternName", patternName);
            alertPayload.put("alertLevel", alertLevel);
            alertPayload.put("beachId", beachId);
            alertPayload.put("details", details);
            alertPayload.put("message", message);
            alertPayload.put("timestamp", java.time.Instant.now().toString());

            String json = mapper.writeValueAsString(alertPayload);
            String topic = "alerts/" + beachId + "/" + alertLevel;

            MqttMessage mqttMessage = new MqttMessage(json.getBytes());
            mqttMessage.setQos(1);
            mqttClient.publish(topic, mqttMessage);

            System.out.println("[ALERT] " + alertLevel + " -> " + topic + ": " + message);
        } catch (MqttException | com.fasterxml.jackson.core.JsonProcessingException e) {
            System.err.println("Error publishing alert: " + e.getMessage());
        }
    }
}
