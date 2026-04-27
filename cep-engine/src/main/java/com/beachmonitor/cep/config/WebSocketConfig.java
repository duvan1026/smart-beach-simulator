package com.beachmonitor.cep.config;

import com.beachmonitor.cep.websocket.SensorWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SensorWebSocketHandler sensorHandler;

    public WebSocketConfig(SensorWebSocketHandler sensorHandler) {
        this.sensorHandler = sensorHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sensorHandler, "/ws/sensors")
                .setAllowedOrigins("*");
    }
}
