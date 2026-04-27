package com.beachmonitor.simulator.service;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

@Service
public class MqttPublisherService {

    private final MqttClient mqttClient;

    public MqttPublisherService(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    public void publish(String message, String topic) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            mqttClient.publish(topic, mqttMessage);
        } catch (MqttException e) {
            System.err.println("Error publishing to " + topic + ": " + e.getMessage());
        }
    }
}
