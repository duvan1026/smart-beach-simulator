-- ============================================================
-- Beach Monitor DB - Schema & Seed Data
-- ============================================================

CREATE SCHEMA IF NOT EXISTS beach_monitor;

-- ----------------------------------------------------------
-- Tabla: esper_patterns (patrones CEP - NO hard-coded en Java)
-- ----------------------------------------------------------
CREATE TABLE beach_monitor.esper_patterns (
    id              BIGSERIAL PRIMARY KEY,
    pattern_id      VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    epl_statement   TEXT         NOT NULL,
    alert_level     VARCHAR(10)  NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------
-- Tabla: beaches (configuracion de playas)
-- ----------------------------------------------------------
CREATE TABLE beach_monitor.beaches (
    id              BIGSERIAL PRIMARY KEY,
    beach_id        VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    lat             DOUBLE PRECISION,
    lon             DOUBLE PRECISION,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ----------------------------------------------------------
-- Tabla: sensor_types (configuracion de tipos de sensor)
-- ----------------------------------------------------------
CREATE TABLE beach_monitor.sensor_types (
    id              BIGSERIAL PRIMARY KEY,
    sensor_type     VARCHAR(20)  NOT NULL UNIQUE,
    unit            VARCHAR(10)  NOT NULL,
    normal_min      DOUBLE PRECISION NOT NULL,
    normal_max      DOUBLE PRECISION NOT NULL,
    variation_min   DOUBLE PRECISION NOT NULL,
    variation_max   DOUBLE PRECISION NOT NULL
);

-- ----------------------------------------------------------
-- Tabla: scenarios (configuracion de escenarios meteorologicos)
-- ----------------------------------------------------------
CREATE TABLE beach_monitor.scenarios (
    id              BIGSERIAL PRIMARY KEY,
    scenario_name   VARCHAR(30)  NOT NULL,
    sensor_type     VARCHAR(20)  NOT NULL,
    min_value       DOUBLE PRECISION NOT NULL,
    max_value       DOUBLE PRECISION NOT NULL,
    variation_min   DOUBLE PRECISION NOT NULL,
    variation_max   DOUBLE PRECISION NOT NULL,
    UNIQUE (scenario_name, sensor_type)
);

-- ----------------------------------------------------------
-- Tabla: sensor_readings (eventos simples)
-- ----------------------------------------------------------
CREATE TABLE beach_monitor.sensor_readings (
    id              BIGSERIAL PRIMARY KEY,
    beach_id        VARCHAR(50)  NOT NULL,
    sensor_type     VARCHAR(20)  NOT NULL,
    value           DOUBLE PRECISION NOT NULL,
    unit            VARCHAR(10)  NOT NULL,
    reading_time    TIMESTAMP    NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sr_beach   ON beach_monitor.sensor_readings(beach_id);
CREATE INDEX idx_sr_type    ON beach_monitor.sensor_readings(sensor_type);
CREATE INDEX idx_sr_time    ON beach_monitor.sensor_readings(reading_time);

-- ----------------------------------------------------------
-- Tabla: complex_events (eventos complejos detectados por CEP)
-- ----------------------------------------------------------
CREATE TABLE beach_monitor.complex_events (
    id              BIGSERIAL PRIMARY KEY,
    pattern_id      VARCHAR(50)  NOT NULL,
    pattern_name    VARCHAR(100) NOT NULL,
    alert_level     VARCHAR(10)  NOT NULL,
    beach_id        VARCHAR(50)  NOT NULL,
    details         JSONB,
    message         TEXT,
    detected_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ce_beach   ON beach_monitor.complex_events(beach_id);
CREATE INDEX idx_ce_level   ON beach_monitor.complex_events(alert_level);
CREATE INDEX idx_ce_time    ON beach_monitor.complex_events(detected_at);

-- ----------------------------------------------------------
-- Vista: ultima alerta por playa
-- ----------------------------------------------------------
CREATE VIEW beach_monitor.latest_alerts AS
SELECT DISTINCT ON (beach_id)
    id, pattern_id, pattern_name, alert_level, beach_id, details, message, detected_at
FROM beach_monitor.complex_events
ORDER BY beach_id, detected_at DESC;

-- ============================================================
-- SEED: 8 patrones EPL
-- ============================================================

INSERT INTO beach_monitor.esper_patterns (pattern_id, name, description, epl_statement, alert_level) VALUES

-- 1. Yellow Alert: viento moderado + presion baja
('yellow_alert',
 'Yellow Weather Alert',
 'Moderate wind with low atmospheric pressure detected',
 '@name(''YellowAlert'') select beachId, windSpeed, pressure, temperature, seaLevel, occupancy, timestamp from BeachCombinedEvent#time(30 sec) where windSpeed > 40.0 and windSpeed <= 70.0 and pressure < 1010.0 and pressure >= 1000.0',
 'YELLOW'),

-- 2. Orange Alert: viento alto + presion significativamente baja + oleaje elevado
('orange_alert',
 'Orange Weather Alert',
 'High wind combined with significant pressure drop and elevated sea level',
 '@name(''OrangeAlert'') select beachId, windSpeed, pressure, seaLevel, temperature, occupancy, timestamp from BeachCombinedEvent#time(30 sec) where windSpeed > 70.0 and windSpeed <= 100.0 and pressure < 1000.0 and pressure >= 980.0 and seaLevel > 1.5',
 'ORANGE'),

-- 3. Red Alert: condiciones extremas
('red_alert',
 'Red Weather Alert',
 'Extreme conditions detected in any parameter',
 '@name(''RedAlert'') select beachId, windSpeed, pressure, temperature, seaLevel, occupancy, timestamp from BeachCombinedEvent#time(30 sec) where windSpeed > 100.0 or pressure < 980.0 or seaLevel > 2.5',
 'RED'),

-- 4. Beach Closure: alerta roja o alta ocupacion durante alerta
('beach_closure',
 'Beach Closure Required',
 'Red conditions or extreme occupancy during any alert level',
 '@name(''BeachClosure'') select a.beachId as beachId, a.windSpeed as windSpeed, a.pressure as pressure, a.occupancy as occupancy, a.seaLevel as seaLevel, a.timestamp as timestamp from pattern [every a=BeachCombinedEvent(windSpeed > 100.0 or pressure < 980.0 or seaLevel > 2.5 or (occupancy > 85 and (windSpeed > 40.0 or pressure < 1010.0)))]',
 'RED'),

-- 5. Heat Wave: temperatura alta sostenida
('heat_wave',
 'Heat Wave Detection',
 'Sustained high temperature over a 2-minute window',
 '@name(''HeatWave'') select beachId, avg(temperature) as avgTemp, max(temperature) as maxTemp, count(*) as readings from BeachCombinedEvent#time(2 min) group by beachId having avg(temperature) > 38.0 and count(*) >= 3',
 'ORANGE'),

-- 6. Storm Approaching: caida rapida de presion
('storm_approaching',
 'Storm Approaching Pattern',
 'Rapid pressure drop detected between consecutive readings',
 '@name(''StormApproaching'') select a.beachId as beachId, a.pressure as currentPressure, b.pressure as previousPressure, a.timestamp as timestamp from pattern [every b=BeachCombinedEvent -> a=BeachCombinedEvent(beachId=b.beachId and pressure < b.pressure - 5.0) where timer:within(60 sec)]',
 'YELLOW'),

-- 7. Levante Wind: viento fuerte sostenido
('levante_wind',
 'Levante Wind Detection',
 'Sustained strong east wind characteristic of Cadiz',
 '@name(''LevanteDetected'') select beachId, avg(windSpeed) as avgWind, max(windSpeed) as maxWind, count(*) as readings from BeachCombinedEvent#time(1 min) group by beachId having avg(windSpeed) > 50.0 and count(*) >= 5',
 'YELLOW'),

-- 8. Regional Alert: multiples playas con viento alto
('multi_beach_alert',
 'Regional Alert (Multiple Beaches)',
 'Two different beaches report high wind within 30 seconds',
 '@name(''RegionalAlert'') select a.beachId as beach1, b.beachId as beach2, a.windSpeed as wind1, b.windSpeed as wind2 from pattern [every a=BeachCombinedEvent(windSpeed > 60.0) -> b=BeachCombinedEvent(windSpeed > 60.0 and beachId != a.beachId) where timer:within(30 sec)]',
 'ORANGE');

-- ============================================================
-- SEED: Playas
-- ============================================================
INSERT INTO beach_monitor.beaches (beach_id, name, lat, lon) VALUES
('victoria',   'Playa de la Victoria',   36.5062, -6.2791),
('caleta',     'Playa de la Caleta',     36.5313, -6.3059),
('cortadura',  'Playa de Cortadura',     36.4878, -6.2676);

-- ============================================================
-- SEED: Tipos de sensor
-- ============================================================
INSERT INTO beach_monitor.sensor_types (sensor_type, unit, normal_min, normal_max, variation_min, variation_max) VALUES
('wind',        'km/h', 5.0,    25.0,   1.0,  3.0),
('pressure',    'hPa',  1010.0, 1025.0, 0.1,  0.5),
('temperature', 'C',    18.0,   28.0,   0.1,  0.3),
('sealevel',    'm',    0.3,    1.2,    0.01, 0.05),
('occupancy',   '%',    0,      100,    1,    5);

-- ============================================================
-- SEED: Escenarios meteorologicos
-- ============================================================
INSERT INTO beach_monitor.scenarios (scenario_name, sensor_type, min_value, max_value, variation_min, variation_max) VALUES
-- NORMAL
('NORMAL', 'wind',        5.0,    25.0,   1.0,  3.0),
('NORMAL', 'pressure',    1010.0, 1025.0, 0.1,  0.5),
('NORMAL', 'temperature', 18.0,   28.0,   0.1,  0.3),
('NORMAL', 'sealevel',    0.3,    1.2,    0.01, 0.05),
('NORMAL', 'occupancy',   0,      100,    1,    5),
-- LEVANTE
('LEVANTE', 'wind',        40.0,  80.0,   5.0,  15.0),
('LEVANTE', 'pressure',    1000.0,1015.0, 0.3,  1.0),
('LEVANTE', 'temperature', 25.0,  35.0,   0.2,  0.5),
('LEVANTE', 'sealevel',    0.5,   1.8,    0.02, 0.1),
('LEVANTE', 'occupancy',   0,     30,     2,    8),
-- STORM
('STORM', 'wind',        70.0,  100.0,  5.0,  10.0),
('STORM', 'pressure',    985.0, 1000.0, 1.0,  3.0),
('STORM', 'temperature', 15.0,  22.0,   0.3,  0.8),
('STORM', 'sealevel',    1.5,   2.5,    0.05, 0.15),
('STORM', 'occupancy',   0,     10,     1,    3),
-- SEVERE_STORM
('SEVERE_STORM', 'wind',        100.0, 140.0, 8.0,  20.0),
('SEVERE_STORM', 'pressure',    960.0, 980.0, 2.0,  5.0),
('SEVERE_STORM', 'temperature', 12.0,  18.0,  0.5,  1.0),
('SEVERE_STORM', 'sealevel',    2.5,   4.0,   0.1,  0.3),
('SEVERE_STORM', 'occupancy',   0,     5,     0,    2),
-- HEATWAVE
('HEATWAVE', 'wind',        3.0,   15.0,   0.5, 2.0),
('HEATWAVE', 'pressure',    1015.0,1030.0, 0.1, 0.3),
('HEATWAVE', 'temperature', 38.0,  45.0,   0.2, 0.5),
('HEATWAVE', 'sealevel',    0.3,   1.0,    0.01,0.03),
('HEATWAVE', 'occupancy',   60,    95,     2,   5),
-- HIGH_OCCUPANCY
('HIGH_OCCUPANCY', 'wind',        30.0,  50.0,   2.0,  5.0),
('HIGH_OCCUPANCY', 'pressure',    1005.0,1015.0, 0.2,  0.5),
('HIGH_OCCUPANCY', 'temperature', 25.0,  32.0,   0.2,  0.4),
('HIGH_OCCUPANCY', 'sealevel',    0.5,   1.5,    0.02, 0.05),
('HIGH_OCCUPANCY', 'occupancy',   85,    100,    1,    3);
