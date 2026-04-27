package com.beachmonitor.simulator.controller;

import com.beachmonitor.simulator.model.OccupancyParams;
import com.beachmonitor.simulator.model.WeatherScenario;
import com.beachmonitor.simulator.service.SensorSimulatorJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@Tag(name = "Simulator Control", description = "Endpoints para controlar el simulador de sensores de playa")
public class ScenarioController {

    private final SensorSimulatorJob simulatorJob;

    public ScenarioController(SensorSimulatorJob simulatorJob) {
        this.simulatorJob = simulatorJob;
    }

    @Operation(
        summary = "Activar escenario meteorologico",
        description = "Cambia el escenario activo del simulador. Todos los sensores de las 3 playas pasaran a generar valores dentro de los rangos del escenario seleccionado.",
        requestBody = @RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(name = "Normal",        value = "{\"scenario\": \"NORMAL\"}"),
                    @ExampleObject(name = "Levante",       value = "{\"scenario\": \"LEVANTE\"}"),
                    @ExampleObject(name = "Tormenta",      value = "{\"scenario\": \"STORM\"}"),
                    @ExampleObject(name = "T. Severa",     value = "{\"scenario\": \"SEVERE_STORM\"}"),
                    @ExampleObject(name = "Ola de Calor",  value = "{\"scenario\": \"HEATWAVE\"}"),
                    @ExampleObject(name = "Alta Ocupacion",value = "{\"scenario\": \"HIGH_OCCUPANCY\"}")
                }
            )
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "Escenario activado correctamente",
                content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": \"ok\", \"scenario\": \"STORM\"}"))),
            @ApiResponse(responseCode = "400", description = "Nombre de escenario desconocido",
                content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Unknown scenario: FOO. Valid: NORMAL, LEVANTE, STORM, SEVERE_STORM, HEATWAVE, HIGH_OCCUPANCY\"}")))
        }
    )
    @PostMapping("/scenario")
    public ResponseEntity<Map<String, String>> setScenario(@org.springframework.web.bind.annotation.RequestBody Map<String, String> request) {
        String scenarioName = request.get("scenario");
        try {
            WeatherScenario scenario = WeatherScenario.valueOf(scenarioName.toUpperCase());
            simulatorJob.setScenario(scenario);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "scenario", scenario.name()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Unknown scenario: " + scenarioName +
                            ". Valid: NORMAL, LEVANTE, STORM, SEVERE_STORM, HEATWAVE, HIGH_OCCUPANCY"
            ));
        }
    }

    @Operation(
        summary = "Consultar escenario activo",
        description = "Devuelve el nombre del escenario meteorologico que esta activo en este momento.",
        responses = @ApiResponse(responseCode = "200", description = "Escenario actual",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\"scenario\": \"NORMAL\"}")))
    )
    @GetMapping("/scenario")
    public ResponseEntity<Map<String, String>> getScenario() {
        return ResponseEntity.ok(Map.of(
                "scenario", simulatorJob.getCurrentScenario().name()
        ));
    }

    @Operation(
        summary = "Actualizar parametros de ocupacion",
        description = "Ajusta la hora simulada y el dia de la semana que usa el simulador para calcular la ocupacion base. " +
                "La temperatura no es un parametro: esta controlada por el escenario activo.",
        requestBody = @RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OccupancyParams.class),
                examples = {
                    @ExampleObject(name = "Mediodia sabado",  value = "{\"hour\": 13, \"dayOfWeek\": \"SATURDAY\"}"),
                    @ExampleObject(name = "Manana lunes",     value = "{\"hour\": 9,  \"dayOfWeek\": \"MONDAY\"}"),
                    @ExampleObject(name = "Noche domingo",    value = "{\"hour\": 22, \"dayOfWeek\": \"SUNDAY\"}")
                }
            )
        ),
        responses = @ApiResponse(responseCode = "200", description = "Parametros actualizados",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\"status\": \"ok\", \"hour\": 13, \"dayOfWeek\": \"SATURDAY\", \"calculatedBaseOccupancy\": 100}")))
    )
    @PostMapping("/occupancy-params")
    public ResponseEntity<Map<String, Object>> setOccupancyParams(@org.springframework.web.bind.annotation.RequestBody OccupancyParams params) {
        simulatorJob.setOccupancyParams(params);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ok");
        response.put("hour", params.getHour());
        response.put("dayOfWeek", params.getDayOfWeek());
        response.put("calculatedBaseOccupancy", Math.round(params.calculateBaseOccupancy()));
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Consultar parametros de ocupacion actuales",
        description = "Devuelve la hora simulada, el dia de la semana y la ocupacion base calculada con los parametros actuales.",
        responses = @ApiResponse(responseCode = "200", description = "Parametros actuales",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\"hour\": 12, \"dayOfWeek\": \"MONDAY\", \"calculatedBaseOccupancy\": 95}")))
    )
    @GetMapping("/occupancy-params")
    public ResponseEntity<Map<String, Object>> getOccupancyParams() {
        OccupancyParams params = simulatorJob.getOccupancyParams();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("hour", params.getHour());
        response.put("dayOfWeek", params.getDayOfWeek());
        response.put("calculatedBaseOccupancy", Math.round(params.calculateBaseOccupancy()));
        return ResponseEntity.ok(response);
    }
}
