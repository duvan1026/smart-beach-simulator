package com.beachmonitor.cep.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SensorWebSocketHandler extends TextWebSocketHandler {

    // Wrap sessions with ConcurrentWebSocketSessionDecorator for thread-safe sending
    private static final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Wrap with concurrent decorator: 5s send timeout, 64KB buffer limit
        WebSocketSession concurrentSession = new ConcurrentWebSocketSessionDecorator(session, 5000, 65536);
        sessions.add(concurrentSession);
        System.out.println("[WS] Client connected: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.removeIf(s -> {
            if (s instanceof ConcurrentWebSocketSessionDecorator d) {
                return d.getDelegate().getId().equals(session.getId());
            }
            return s.getId().equals(session.getId());
        });
        System.out.println("[WS] Client disconnected: " + session.getId());
    }

    public void broadcastReadings(Map<String, Object> eventMap) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "readings");
        message.put("data", eventMap);
        broadcast(message);
    }

    public void broadcastAlert(String patternId, String patternName, String alertLevel,
                               String beachId, Map<String, Object> details, String msg) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "alert");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("patternId", patternId);
        data.put("patternName", patternName);
        data.put("alertLevel", alertLevel);
        data.put("beachId", beachId);
        data.put("details", details);
        data.put("message", msg);
        data.put("timestamp", java.time.Instant.now().toString());
        message.put("data", data);
        broadcast(message);
    }

    private void broadcast(Map<String, Object> message) {
        if (sessions.isEmpty()) return;
        try {
            String json = mapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                    }
                } catch (Exception e) {
                    // Remove broken sessions
                    sessions.remove(session);
                }
            }
        } catch (Exception e) {
            System.err.println("[WS] Broadcast error: " + e.getMessage());
        }
    }
}
