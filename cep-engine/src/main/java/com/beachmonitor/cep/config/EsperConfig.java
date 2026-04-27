package com.beachmonitor.cep.config;

import com.beachmonitor.cep.listener.GenericPatternListener;
import com.beachmonitor.cep.persistence.entity.EsperPatternEntity;
import com.beachmonitor.cep.persistence.repository.EsperPatternRepository;
import com.beachmonitor.cep.persistence.service.PersistenceService;
import com.beachmonitor.cep.publisher.MqttAlertPublisher;
import com.beachmonitor.cep.websocket.SensorWebSocketHandler;
import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompiler;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@org.springframework.context.annotation.Configuration
public class EsperConfig {

    @Value("${esper.eventTypesPath:/app/config/event-types.json}")
    private String eventTypesPath;

    private final EsperPatternRepository patternRepository;
    private final PersistenceService persistenceService;
    private final MqttAlertPublisher alertPublisher;
    private final SensorWebSocketHandler webSocketHandler;

    private Configuration esperConfiguration;
    private EPRuntime runtime;

    // patternId -> deploymentId
    private final Map<String, String> deploymentIds = new ConcurrentHashMap<>();

    public EsperConfig(EsperPatternRepository patternRepository,
                       PersistenceService persistenceService,
                       MqttAlertPublisher alertPublisher,
                       SensorWebSocketHandler webSocketHandler) {
        this.patternRepository = patternRepository;
        this.persistenceService = persistenceService;
        this.alertPublisher = alertPublisher;
        this.webSocketHandler = webSocketHandler;
    }

    @Bean
    public EPRuntime esperRuntime() throws Exception {
        esperConfiguration = new Configuration();
        loadEventTypes(esperConfiguration);

        runtime = EPRuntimeProvider.getDefaultRuntime(esperConfiguration);
        loadAndDeployPatterns(runtime);

        System.out.println("Esper CEP engine initialized successfully");
        return runtime;
    }

    private void loadEventTypes(Configuration config) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is;

        File externalFile = new File(eventTypesPath);
        if (externalFile.exists()) {
            is = externalFile.toURI().toURL().openStream();
        } else {
            is = getClass().getClassLoader().getResourceAsStream("event-types.json");
        }

        JsonNode root = mapper.readTree(is);
        JsonNode eventTypes = root.get("eventTypes");

        for (JsonNode et : eventTypes) {
            String name = et.get("name").asText();
            Map<String, Object> properties = new HashMap<>();

            et.get("properties").fieldNames().forEachRemaining(field -> {
                String type = et.get("properties").get(field).asText();
                switch (type) {
                    case "string" -> properties.put(field, String.class);
                    case "double" -> properties.put(field, double.class);
                    case "int" -> properties.put(field, int.class);
                    case "long" -> properties.put(field, long.class);
                    default -> properties.put(field, String.class);
                }
            });

            config.getCommon().addEventType(name, properties);
            System.out.println("Registered Esper event type: " + name + " with " + properties.size() + " properties");
        }
    }

    private void loadAndDeployPatterns(EPRuntime rt) {
        List<EsperPatternEntity> patterns = patternRepository.findByEnabledTrue();
        System.out.println("Loading " + patterns.size() + " EPL patterns from database...");

        for (EsperPatternEntity pattern : patterns) {
            deployPattern(rt, pattern);
        }
    }

    private void deployPattern(EPRuntime rt, EsperPatternEntity pattern) {
        try {
            EPCompiler compiler = EPCompilerProvider.getCompiler();
            CompilerArguments args = new CompilerArguments(esperConfiguration);
            EPCompiled compiled = compiler.compile(pattern.getEplStatement(), args);
            EPDeployment deployment = rt.getDeploymentService().deploy(compiled);

            deploymentIds.put(pattern.getPatternId(), deployment.getDeploymentId());

            for (EPStatement stmt : deployment.getStatements()) {
                stmt.addListener(new GenericPatternListener(
                        pattern.getPatternId(),
                        pattern.getName(),
                        pattern.getAlertLevel(),
                        persistenceService,
                        alertPublisher,
                        webSocketHandler
                ));
            }
            System.out.println("  Deployed pattern: " + pattern.getPatternId() +
                    " (" + pattern.getName() + ") [" + pattern.getAlertLevel() + "]");
        } catch (Exception e) {
            System.err.println("  FAILED to deploy pattern " + pattern.getPatternId() +
                    ": " + e.getMessage());
        }
    }

    public void enablePattern(EsperPatternEntity pattern) {
        if (deploymentIds.containsKey(pattern.getPatternId())) return;
        deployPattern(runtime, pattern);
    }

    public void disablePattern(String patternId) {
        String deploymentId = deploymentIds.remove(patternId);
        if (deploymentId == null) return;
        try {
            runtime.getDeploymentService().undeploy(deploymentId);
            System.out.println("  Undeployed pattern: " + patternId);
        } catch (Exception e) {
            System.err.println("  FAILED to undeploy pattern " + patternId + ": " + e.getMessage());
        }
    }

    public boolean isDeployed(String patternId) {
        return deploymentIds.containsKey(patternId);
    }
}
