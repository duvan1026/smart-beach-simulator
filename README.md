# Beach Monitor — Sistema de Monitoreo de Playas de Cadiz

**Asignatura:** Smart Data: Sistemas y Aplicaciones  
**Universidad de Cadiz** - Curso 2025/2026  
**Autores:** Duvan & Ivan

Sistema de monitoreo inteligente de playas que detecta alertas meteorologicas (amarilla, naranja, roja) mediante procesamiento de eventos complejos (CEP), aplicado a tres playas de Cadiz, Espana.

---

## Arquitectura

```
[Sensor Simulator] --MQTT--> [Mosquitto] --MQTT--> [Node-RED Orchestrator]
   (Spring Boot)                                          |
                                                   (combina 5 sensores
                                                    por playa en un JSON)
                                                          |
                                                    AMQP (RabbitMQ)
                                                          |
                                                  [CEP Engine (Esper)]
                                                    /            \
                                             PostgreSQL      MQTT publish
                                          (eventos simples    (alertas)
                                           + complejos)          |
                                                          [Node-RED Dashboard]
                                                         (graficos + alertas
                                                          + botones control)
```

### Flujo de datos

1. El **Sensor Simulator** genera lecturas cada 3 segundos para 3 playas x 5 sensores y las publica via **MQTT** a Mosquitto.
2. **Node-RED Orchestrator** suscribe a `beach/#`, combina los 5 sensores por playa en un unico JSON y lo publica a **RabbitMQ**.
3. El **CEP Engine** consume mensajes de RabbitMQ, persiste las lecturas simples en **PostgreSQL** y los envia al motor **Esper**.
4. Esper evalua los 8 patrones EPL y cuando detecta una coincidencia, persiste el evento complejo y publica la alerta via MQTT a `alerts/{beachId}/{alertLevel}`.
5. El **Dashboard de Node-RED** muestra graficos en tiempo real, alertas con colores y una tabla de eventos complejos.

---

## Tecnologias

| Tecnologia | Uso |
|---|---|
| **Java 21 + Spring Boot 3.5.7** | Sensor Simulator y CEP Engine |
| **Esper 8.9.0** | Motor de procesamiento de eventos complejos (CEP) |
| **RabbitMQ 3** | Broker de mensajeria AMQP |
| **Eclipse Mosquitto 2** | Broker MQTT (protocolo IoT) |
| **PostgreSQL 16** | Base de datos relacional |
| **Node-RED** | Orquestador de flujos + Dashboard de monitorizacion |
| **Docker + Docker Compose** | Contenerizacion y orquestacion de servicios |

---

## Estructura del Proyecto

```
beach-monitor/
├── docker-compose.yml              # 6 servicios Docker
├── .env                            # Variables de entorno
│
├── config/                         # Configuracion externa (NO hard-coded)
│   ├── sensors-config.json         # Definicion de sensores, playas y escenarios
│   └── event-types.json            # Tipos de eventos registrados en Esper
│
├── sensor-simulator/               # Modulo 1: Simulador de Sensores
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/beachmonitor/simulator/
│       ├── config/
│       ├── model/
│       ├── service/simulators/     # WindSimulator, PressureSimulator, etc.
│       ├── controller/
│       └── utils/
│
├── cep-engine/                     # Modulo 2: Motor CEP
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/beachmonitor/cep/
│       ├── config/
│       ├── engine/                 # EsperEventProcessor
│       ├── listener/               # GenericPatternListener
│       ├── persistence/            # Entidades + repositorios + servicio
│       └── publisher/              # MqttAlertPublisher
│
├── mosquitto/
├── nodered/
│   └── flows/flows.json            # Orquestador + Dashboard
│
├── postgres/
│   └── init.sql                    # Schema, tablas y 8 patrones EPL
│
└── docs/
    ├── TECHNICAL.md                # Documentacion tecnica detallada
    └── database-diagram.html       # Diagrama interactivo de base de datos
```

---

## Requisitos Previos

- **Docker** >= 20.x
- **Docker Compose** >= 2.x
- Puertos libres: 1880, 1883, 5432, 5672, 8290, 8291, 9001, 15672

---

## Como Ejecutar

```bash
cd beach-monitor
docker-compose up --build
```

En segundo plano:

```bash
docker-compose up --build -d
```

Detener:

```bash
docker-compose down
```

Detener y eliminar volumenes (borra datos de PostgreSQL):

```bash
docker-compose down -v
```

---

## Acceso a los Servicios

| Servicio | URL |
|---|---|
| **Dashboard Node-RED** | http://localhost:1880/ui |
| **Editor Node-RED** | http://localhost:1880 |
| **RabbitMQ Management** | http://localhost:15672 (user: `beach` / pass: `monitor`) |
| **Sensor Simulator API** | http://localhost:8290/scenario |
| **Swagger UI** | http://localhost:8290/swagger-ui.html |
| **CEP Engine** | http://localhost:8291 |

---

## Playas Monitorizadas

| Playa | ID | Coordenadas |
|---|---|---|
| Playa de la Victoria | `victoria` | 36.52 N, 6.29 W |
| Playa de la Caleta | `caleta` | 36.53 N, 6.30 W |
| Playa de Cortadura | `cortadura` | 36.49 N, 6.25 W |

---

## Escenarios de Simulacion

| Escenario | Efecto | Alerta CEP Esperada |
|---|---|---|
| `NORMAL` | Todos en rangos normales | Ninguna |
| `LEVANTE` | Viento 40–80 km/h, presion ligeramente baja | Yellow Alert, Levante Wind |
| `STORM` | Viento >70, presion <1000, oleaje >1.5m | Orange Alert, Storm Approaching |
| `SEVERE_STORM` | Viento >100, presion <980, oleaje >2.5m | Red Alert, Beach Closure |
| `HEATWAVE` | Temperatura >38 °C sostenida | Heat Wave (Orange) |
| `HIGH_OCCUPANCY` | Ocupacion >85% + viento moderado | Beach Closure |

```bash
# Activar escenario via API
curl -X POST http://localhost:8290/scenario \
  -H "Content-Type: application/json" \
  -d '{"scenario": "SEVERE_STORM"}'
```

---

## Patrones CEP (resumen)

Los 8 patrones EPL se cargan desde PostgreSQL al arrancar. No estan hard-coded en Java.

| # | Pattern ID | Nombre | Nivel |
|---|---|---|---|
| 1 | `yellow_alert` | Yellow Weather Alert | YELLOW |
| 2 | `orange_alert` | Orange Weather Alert | ORANGE |
| 3 | `red_alert` | Red Weather Alert | RED |
| 4 | `beach_closure` | Beach Closure Required | RED |
| 5 | `heat_wave` | Heat Wave Detection | ORANGE |
| 6 | `storm_approaching` | Storm Approaching | YELLOW |
| 7 | `levante_wind` | Levante Wind Detection | YELLOW |
| 8 | `multi_beach_alert` | Regional Alert | ORANGE |

Para la descripcion completa de cada patron (logica, EPL, operadores) ver [docs/TECHNICAL.md](docs/TECHNICAL.md#patrones-cep--descripcion-detallada).

---

## Verificacion Rapida

```bash
# 1. Estado de los servicios
docker-compose ps

# 2. Lecturas MQTT de sensores
docker exec -it mosquitto mosquitto_sub -t 'beach/#' -v

# 3. Alertas MQTT
docker exec -it mosquitto mosquitto_sub -t 'alerts/#' -v

# 4. Ultimos eventos en PostgreSQL
docker exec -it postgres psql -U beach -d BeachMonitorDB -c \
  "SELECT alert_level, pattern_name, beach_id, message FROM beach_monitor.complex_events ORDER BY detected_at DESC LIMIT 10;"
```

---

## Documentacion Adicional

| Documento | Contenido |
|---|---|
| [docs/TECHNICAL.md](docs/TECHNICAL.md) | Modelado del simulador, patrones CEP detallados, topics MQTT, esquema de base de datos |
| [docs/database-diagram.html](docs/database-diagram.html) | Diagrama interactivo del schema PostgreSQL |
