package com.beachmonitor.simulator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI beachMonitorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Beach Monitor — Sensor Simulator API")
                        .description("API REST para controlar el simulador de sensores de las playas de Cadiz. " +
                                "Permite activar escenarios meteorologicos y ajustar los parametros de simulacion de ocupacion.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Duvan & Ivan")
                                .email("duvan@beachmonitor.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8290").description("Local"),
                        new Server().url("http://sensor-simulator:8290").description("Docker")
                ));
    }
}
