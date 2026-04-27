# Documentacion Tecnica — Beach Monitor

**Asignatura:** Smart Data: Sistemas y Aplicaciones  
**Universidad de Cadiz** - Curso 2025/2026  
**Autores:** Duvan & Ivan

---

## Diagrama de Base de Datos

[Ver diagrama interactivo de base de datos](database-diagram.html)

---

## Modelado del Simulador de Sensores

### Comportamiento base — random walk discreto acotado (*bounded random walk*)

Los sensores meteorologicos (viento, presion, temperatura, nivel del mar) siguen el modelo de **random walk discreto acotado** (*bounded random walk* o *clipped random walk*). En cada paso:

1. Se elige una magnitud de variacion aleatoria uniforme dentro de `[variationMin, variationMax]`.
2. La direccion (subir o bajar) se decide con probabilidad **50/50** (`randomBoolean()` — lanzamiento de moneda sin sesgo).
3. Si el nuevo valor sale del rango del escenario, se recorta (*clamp*) al limite.

Implementado en `SensorBehaviorUtils.nextSensorValue`:

```
variacion     = aleatorio_uniforme(variationMin, variationMax)
direccion     = aleatorio_booleano()          // 50% subir, 50% bajar
valor_nuevo   = valor_anterior ± variacion
valor_nuevo   = clamp(valor_nuevo, min_escenario, max_escenario)
```

Esto produce series temporales continuas y realistas: sin saltos bruscos, pero con deriva natural dentro del rango del escenario.

### Inicializacion

Cada simulador arranca con un valor inicial aleatorio dentro del rango `NORMAL` del sensor. Si el escenario cambia, el rango se actualiza en la siguiente lectura y el valor comienza a converger al nuevo rango de forma gradual (por el random walk).

### Sensor de ocupacion — modelo hibrido basado en reglas con suavizado exponencial

El `OccupancySimulator` combina dos tecnicas:

**Tecnica 1 — Modelo basado en reglas** (*rule-based model*)  
Calcula una ocupacion objetivo a partir de factores contextuales mediante reglas deterministas:

- **Factor hora** (0.0 – 1.0): escala la ocupacion segun la franja horaria simulada.

| Franja horaria | Factor |
|---|---|
| Noche (22h – 6h) | 0.02 |
| Madrugada (7h) | 0.10 |
| Manana (8h – 9h) | 0.30 |
| Media manana (10h – 11h) | 0.55 |
| Mediodia (12h – 14h) | 0.95 |
| Tarde temprana (15h) | 0.85 |
| Tarde (16h – 17h) | 0.65 |
| Tarde-noche (18h – 19h) | 0.40 |
| Noche temprana (20h – 21h) | 0.15 |

- **Factor dia de la semana**: fin de semana multiplica por **1.4**, dias laborables por **1.0**.

La ocupacion base se calcula como:

```
base = factor_hora * factor_dia_semana * 100
base = clamp(base, 0, 100)
```

> La temperatura se excluye deliberadamente de este calculo: ya esta gobernada por el escenario activo (NORMAL, STORM, etc.), incluirla aqui crearia una doble dependencia con los limites del escenario aplicados en el paso siguiente.

**Tecnica 2 — Suavizado exponencial** (*exponential smoothing* / *first-order IIR filter*)  
En lugar de saltar directamente al valor objetivo, el simulador avanza suavemente hacia el usando un coeficiente α = 0.3:

```
target = clamp(base, min_escenario, max_escenario)
nuevo  = actual + (target - actual) * 0.3 + ruido_aleatorio
```

El factor `0.3` (α) controla la velocidad de convergencia: valores cercanos a 0 convergen muy lento; valores cercanos a 1 saltan casi directamente al objetivo. Con α = 0.3 se simula que la gente no llega ni abandona la playa de forma instantanea.

La restriccion por escenario actua sobre el `target`: `STORM` fuerza un maximo bajo (0–10%); `HIGH_OCCUPANCY` fuerza un minimo alto (85–100%), limitando hacia donde puede converger el suavizado.

### Correlacion espacial entre playas

`SensorBehaviorUtils.correlatedValue` permite que los sensores de distintas playas esten correlacionados espacialmente. Usa atenuacion exponencial por distancia y ruido gaussiano:

```
atenuacion = exp(-distancia_metros / 100_000)
valor = valor_base * atenuacion + ruido_gaussiano(0, sigma)
```

Esto modela el hecho de que un frente meteorologico afecta mas a playas cercanas entre si.

### Rangos por escenario

Cada escenario define rangos independientes para los 5 sensores calibrados con datos reales de AEMET (estacion 5973, Cadiz, 2024-2026) y umbrales oficiales del Plan Meteoalerta (METEOALERTA_ANX1, AEMET 2022). La tabla muestra los rangos de generacion:

| Escenario | Viento (km/h) | Presion (hPa) | Temperatura (°C) | Nivel Mar (m) | Ocupacion (%) |
|---|---|---|---|---|---|
| `NORMAL` | 5 – 43 | 1011 – 1024 | 16 – 30 | 0.3 – 1.2 | 0 – 100 |
| `LEVANTE` | 50 – 68 | 1010 – 1017 | 22 – 33 | 0.5 – 1.5 | 0 – 30 |
| `STORM` | 60 – 88 | 997 – 1010 | 12 – 20 | 1.5 – 2.5 | 0 – 10 |
| `SEVERE_STORM` | 91 – 130 | 994 – 997 | 9 – 16 | 2.5 – 4.0 | 0 – 5 |
| `HEATWAVE` | 3 – 20 | 1013 – 1022 | 36 – 42 | 0.3 – 1.0 | 60 – 95 |
| `HIGH_OCCUPANCY` | 20 – 43 | 1011 – 1020 | 22 – 30 | 0.5 – 1.5 | 85 – 100 |

Cada valor dentro del rango se genera con el random walk descrito. La variacion por paso tambien esta parametrizada por escenario (columna `variationMin`/`variationMax` en `sensors-config.json`).

### Trazabilidad de fuentes por escenario y variable

#### Fuentes utilizadas

| Fuente | Descripcion |
|---|---|
| **AEMET-HIST** | Historico diario estacion 5973 Cadiz, 811 dias (2024-01-01 a 2026-03-21). Campos: `tmin`, `tmax`, `velmedia` (m/s→km/h), `racha` (m/s→km/h), `presMax`, `presMin`. Origen: opendata.aemet.es |
| **AEMET-PDF** | METEOALERTA_ANX1 (31-may-2022). Seccion 2.2 Fenomenos costeros zonas mediterraneas/atlanticas. Seccion 3.1 Tabla Andalucia zona 611103 Litoral gaditano |

Los umbrales de viento para definir el inicio de cada escenario de alerta se tomaron de la **seccion 2.2 del PDF** (fenomenos costeros), no de la tabla 3.1 (racha en tierra), porque el sistema monitorea riesgo en playa para banistas y navegantes. Los umbrales costeros son: amarillo ≥ 50 km/h, naranja ≥ 60 km/h, rojo ≥ 90 km/h.

---

#### NORMAL

| Variable | Rango | De donde |
|---|---|---|
| Viento | 5 – 43 km/h | AEMET-HIST: vel_media global p99=41 km/h, max absoluto vel_media=56.2 km/h. El p90 de racha en verano es 58 km/h → el normal llega hasta p75 de racha (~43 km/h) para no solapar con LEVANTE |
| Presion | 1011 – 1024 hPa | AEMET-HIST: pmin_mean=1014.7, pmax_mean=1018.6. Rango p10–p90 de presMax: 1012.7–1026.7 → redondeado a banda tipica observada |
| Temperatura | 16 – 30 °C | AEMET-HIST: tmin global p5=9°C (invierno), tmax verano mean=27.9, max=37.7. Rango 16–30 cubre toda la temporada de playa (may-oct) sin incluir extremos de ola de calor |
| Nivel mar | 0.3 – 1.2 m | Rango mareal tipico de Cadiz (mareas semidiurnas, amplitud ~1 m en condiciones normales). Sin dato directo en el historico; valor mantenido de literatura costero-maritima |
| Ocupacion | 0 – 100 % | Variable controlada por el modelo de reglas de OccupancyParams; sin restriccion en NORMAL |

#### LEVANTE

| Variable | Rango | De donde |
|---|---|---|
| Viento | 50 – 68 km/h | AEMET-PDF sec. 2.2: alerta amarilla costera ≥ 50 km/h. Techo en 68 para no solapar con naranja (≥60). AEMET-HIST: racha p90 verano=58, p95=62 → el levante tipico queda en 50-68 km/h |
| Presion | 1010 – 1017 hPa | AEMET-HIST: en meses de levante (mar-abr) pmin_mean≈1010–1013. Banda ligeramente baja pero sin borrasca activa |
| Temperatura | 22 – 33 °C | AEMET-HIST: el levante eleva temperatura en Cadiz. tmax_mean verano=27.9, max mayo=33.6. Rango cubre dias calurosos con viento este |
| Nivel mar | 0.5 – 1.5 m | Levante genera algo de oleaje pero no extremo. Limite superior antes del umbral de alerta costera (oleaje combinado 3 m en naranja) |
| Ocupacion | 0 – 30 % | El viento fuerte reduce drásticamente la ocupación de playa |

#### STORM

| Variable | Rango | De donde |
|---|---|---|
| Viento | 60 – 88 km/h | AEMET-PDF sec. 2.2: alerta naranja costera ≥ 60 km/h. Techo en 88 para quedar justo por debajo del umbral rojo (≥90). AEMET-HIST: 108 dias con racha ≥60 km/h en 811 dias (13%) |
| Presion | 997 – 1010 hPa | AEMET-HIST: pmin_min global=994.8. En borrascas moderadas pmin baja a 997-1005. Banda inferior a la normal pero sin alcanzar el minimo absoluto historico |
| Temperatura | 12 – 20 °C | AEMET-HIST: tmin_mean invierno=11.8, tmax_mean invierno=17.5. Las tormentas llegan principalmente en invierno/primavera. tmin global min=5.3°C |
| Nivel mar | 1.5 – 2.5 m | Criterio de alerta naranja costera: oleaje combinado > 4-5 m → nivel mar simulado proporcional |
| Ocupacion | 0 – 10 % | Playa practicamente vacia en tormenta |

#### SEVERE_STORM

| Variable | Rango | De donde |
|---|---|---|
| Viento | 91 – 130 km/h | AEMET-PDF sec. 2.2: alerta roja costera ≥ 90 km/h. Minimo en 91 para estar claramente en rojo. AEMET-HIST: max absoluto racha=91.1 km/h (1 unico dia en 811) → 130 km/h es extrapolacion de evento excepcional, fuera del historico observado |
| Presion | 994 – 997 hPa | AEMET-HIST: pmin absoluto historico=994.8 hPa (minimo de toda la serie). Banda en torno al minimo registrado |
| Temperatura | 9 – 16 °C | AEMET-HIST: tmin global min=5.3°C, tmin_mean invierno=11.8. Temperatura fria asociada a borrasca profunda en invierno |
| Nivel mar | 2.5 – 4.0 m | AEMET-PDF sec. 2.2: rojo → oleaje combinado > 7-8 m en atlantico. Nivel simulado proporcional para playa |
| Ocupacion | 0 – 5 % | Playa cerrada o practicamente evacuada |

#### HEATWAVE

| Variable | Rango | De donde |
|---|---|---|
| Viento | 3 – 20 km/h | AEMET-HIST: en dias de calor extremo el viento es calmo. vel_media global p5=6.1 km/h. Rango bajo para simular calma chicha tipica de ola de calor |
| Presion | 1013 – 1022 hPa | AEMET-HIST: pmax_mean verano=1015.8. En ola de calor hay alta presion → banda alta dentro del rango normal-alto |
| Temperatura | 36 – 42 °C | AEMET-PDF sec. 3.1 zona 611103 Litoral gaditano: alerta amarilla tmax=36°C, naranja=39°C, rojo=42°C. AEMET-HIST: max absoluto tmax=37.7°C → rango cubre desde el umbral amarillo hasta el rojo oficial |
| Nivel mar | 0.3 – 1.0 m | Sin viento ni borrascas, el nivel del mar es bajo y calmado |
| Ocupacion | 60 – 95 % | Alta afluencia en verano caluroso |

#### HIGH_OCCUPANCY

| Variable | Rango | De donde |
|---|---|---|
| Viento | 20 – 43 km/h | AEMET-HIST: vel_media global mean=16.2, p75=20.2, p90=27. Viento moderado presente pero sin llegar a umbral de alerta (< 50 km/h). Rango tipico de dia de playa con viento normal-alto |
| Presion | 1011 – 1020 hPa | AEMET-HIST: rango tipico de dias buenos de verano. pmin_mean verano=1012.8, pmax_mean verano=1015.8 |
| Temperatura | 22 – 30 °C | AEMET-HIST: tmax_mean verano=27.9, tmin_mean verano=21.9. Dias agradables de playa sin llegar a ola de calor (< 36°C) |
| Nivel mar | 0.5 – 1.5 m | Nivel normal-algo elevado en dia de verano con viento moderado |
| Ocupacion | 85 – 100 % | Playa saturada, fin de semana o festivo en temporada alta |

---

## Patrones CEP — Descripcion Detallada

Los 8 patrones EPL se cargan desde `beach_monitor.esper_patterns` al arrancar el CEP Engine. Ninguno esta hard-coded en Java. A continuacion se describe la logica y los operadores Esper de cada uno.

---

### 1. Yellow Alert — `yellow_alert`

**Nivel:** YELLOW  
**Condicion:** Viento moderado (40–70 km/h) combinado con presion ligeramente baja (1000–1010 hPa), sostenido en una ventana de 30 segundos.

**EPL:**
```sql
@name('YellowAlert')
select beachId, windSpeed, pressure, temperature, seaLevel, occupancy, timestamp
from BeachCombinedEvent#time(30 sec)
where windSpeed > 40.0 and windSpeed <= 70.0
  and pressure < 1010.0 and pressure >= 1000.0
```

**Operadores:** ventana temporal `#time(30 sec)`, filtro `AND`, comparaciones de rango.

**Interpretacion:** Detecta condiciones de viento levante tipico de Cadiz. No es peligroso por si solo, pero anticipa deterioro. Se activa con el escenario `LEVANTE`.

---

### 2. Orange Alert — `orange_alert`

**Nivel:** ORANGE  
**Condicion:** Viento alto (70–100 km/h) con caida significativa de presion (980–1000 hPa) y oleaje elevado (> 1.5 m), en ventana de 30 segundos.

**EPL:**
```sql
@name('OrangeAlert')
select beachId, windSpeed, pressure, seaLevel, temperature, occupancy, timestamp
from BeachCombinedEvent#time(30 sec)
where windSpeed > 70.0 and windSpeed <= 100.0
  and pressure < 1000.0 and pressure >= 980.0
  and seaLevel > 1.5
```

**Operadores:** `#time(30 sec)`, `AND`, comparaciones multiples sobre tres campos simultaneos.

**Interpretacion:** La combinacion de los tres factores indica una tormenta desarrollada. Solo alerta cuando los tres se cumplen a la vez, evitando falsos positivos por un unico sensor elevado.

---

### 3. Red Alert — `red_alert`

**Nivel:** RED  
**Condicion:** Cualquier parametro alcanza niveles extremos: viento > 100 km/h, presion < 980 hPa, o nivel del mar > 2.5 m.

**EPL:**
```sql
@name('RedAlert')
select beachId, windSpeed, pressure, temperature, seaLevel, occupancy, timestamp
from BeachCombinedEvent#time(30 sec)
where windSpeed > 100.0 or pressure < 980.0 or seaLevel > 2.5
```

**Operadores:** `#time(30 sec)`, `OR` — basta que uno de los tres parametros supere el umbral extremo.

**Interpretacion:** Condicion de emergencia. A diferencia de Orange (que requiere tres condiciones simultaneas), aqui el `OR` actua como "cualquiera de los factores por si solo ya es peligroso".

---

### 4. Beach Closure — `beach_closure`

**Nivel:** RED  
**Condicion:** Condiciones extremas de tormenta (`OR` de tres factores) o alta ocupacion con viento moderado.

**EPL:**
```sql
@name('BeachClosure')
select a.beachId, a.windSpeed, a.pressure, a.occupancy, a.seaLevel, a.timestamp
from pattern [every a=BeachCombinedEvent(
    windSpeed > 100.0 or pressure < 980.0 or seaLevel > 2.5
    or (occupancy > 85 and (windSpeed > 40.0 or pressure < 1010.0))
)]
```

**Operadores:** `pattern [every ...]` — re-dispara por cada evento que cumpla la condicion (no solo la primera vez). Logica compuesta `OR` + `AND` anidados.

**Interpretacion:** Este patron cubre dos causas de cierre distintas:  
- Condiciones meteorologicas extremas (misma logica que Red Alert).  
- Playa saturada en condiciones meteorologicas adversas, aunque no extremas: alta ocupacion + viento moderado es peligroso por el riesgo de accidentes.

El `every` es clave: permite que el patron siga disparandose mientras la condicion persiste, no solo la primera vez.

---

### 5. Heat Wave — `heat_wave`

**Nivel:** ORANGE  
**Condicion:** Temperatura media > 38 °C sostenida durante 2 minutos con al menos 3 lecturas.

**EPL:**
```sql
@name('HeatWave')
select beachId, avg(temperature) as avgTemp, max(temperature) as maxTemp, count(*) as readings
from BeachCombinedEvent#time(2 min)
group by beachId
having avg(temperature) > 38.0 and count(*) >= 3
```

**Operadores:** `#time(2 min)`, agregacion `avg()` + `max()` + `count(*)`, `group by beachId`, `having`.

**Interpretacion:** A diferencia de los patrones anteriores (que miran eventos individuales), este patron evalua una **ventana de agregacion por playa**. Requiere que la media sostenida en 2 minutos supere el umbral, filtrando picos puntuales de temperatura. El `group by` permite detectar ola de calor por playa de forma independiente. El `having count(*) >= 3` garantiza que hay suficientes datos antes de disparar.

---

### 6. Storm Approaching — `storm_approaching`

**Nivel:** YELLOW  
**Condicion:** Caida rapida de presion entre dos lecturas consecutivas de la misma playa (> 5 hPa de bajada) dentro de 60 segundos.

**EPL:**
```sql
@name('StormApproaching')
select a.beachId, a.pressure as currentPressure, b.pressure as previousPressure, a.timestamp
from pattern [every b=BeachCombinedEvent
    -> a=BeachCombinedEvent(beachId=b.beachId and pressure < b.pressure - 5.0)
    where timer:within(60 sec)]
```

**Operadores:** `pattern [every b -> a]` (followed-by), `timer:within(60 sec)`, correlacion entre eventos `beachId=b.beachId`, aritmetica de campo `b.pressure - 5.0`.

**Interpretacion:** El operador `->` (followed-by) captura la secuencia temporal: primero llega un evento `b`, luego debe llegar un evento `a` de la **misma playa** con una presion al menos 5 hPa inferior. El `timer:within` invalida la correlacion si no llega el segundo evento en 60 segundos. Permite detectar la firma de aproximacion de frentes meteorologicos antes de que las condiciones se vuelvan peligrosas.

---

### 7. Levante Wind — `levante_wind`

**Nivel:** YELLOW  
**Condicion:** Viento medio > 50 km/h sostenido durante 1 minuto con al menos 5 lecturas en la playa.

**EPL:**
```sql
@name('LevanteDetected')
select beachId, avg(windSpeed) as avgWind, max(windSpeed) as maxWind, count(*) as readings
from BeachCombinedEvent#time(1 min)
group by beachId
having avg(windSpeed) > 50.0 and count(*) >= 5
```

**Operadores:** `#time(1 min)`, `avg()`, `max()`, `count(*)`, `group by beachId`, `having`.

**Interpretacion:** Patron de agregacion similar a Heat Wave pero sobre viento y con ventana de 1 minuto. Diferencia un pico de viento puntual de una condicion de levante real (viento sostenido). El threshold de 5 lecturas en 60 segundos (una cada 12 segundos) asegura continuidad real. Especifico del contexto de Cadiz, donde el viento levante es un fenomeno meteorologico frecuente.

---

### 8. Regional Alert — `multi_beach_alert`

**Nivel:** ORANGE  
**Condicion:** Dos playas **distintas** reportan viento > 60 km/h dentro de un margen de 30 segundos entre si.

**EPL:**
```sql
@name('RegionalAlert')
select a.beachId as beach1, b.beachId as beach2, a.windSpeed as wind1, b.windSpeed as wind2
from pattern [every a=BeachCombinedEvent(windSpeed > 60.0)
    -> b=BeachCombinedEvent(windSpeed > 60.0 and beachId != a.beachId)
    where timer:within(30 sec)]
```

**Operadores:** `pattern [every a -> b]`, `timer:within(30 sec)`, correlacion cross-event `beachId != a.beachId`.

**Interpretacion:** El unico patron de correlacion entre playas. Detecta que la condicion de viento alto es **regional** (afecta a mas de una playa), no un fallo puntual de un sensor. La expresion `beachId != a.beachId` garantiza que `b` es una playa diferente a `a`. Esto distingue un evento local de un frente que recorre toda la costa. Clasifica como ORANGE porque la dimension regional implica mayor riesgo para el sistema costero.

---

## Estructura de Datos

### Schema PostgreSQL: `beach_monitor`

#### `esper_patterns` — Patrones CEP (configuracion)

| Columna | Tipo | Descripcion |
|---|---|---|
| id | BIGSERIAL PK | ID autoincremental |
| pattern_id | VARCHAR(50) UNIQUE | Identificador del patron |
| name | VARCHAR(100) | Nombre descriptivo |
| description | TEXT | Descripcion del patron |
| epl_statement | TEXT | Sentencia EPL de Esper |
| alert_level | VARCHAR(10) | YELLOW, ORANGE o RED |
| enabled | BOOLEAN | Si el patron esta activo |
| created_at | TIMESTAMP | Fecha de creacion |
| updated_at | TIMESTAMP | Fecha de actualizacion |

#### `sensor_readings` — Eventos simples

| Columna | Tipo | Descripcion |
|---|---|---|
| id | BIGSERIAL PK | ID autoincremental |
| beach_id | VARCHAR(50) | ID de la playa |
| sensor_type | VARCHAR(20) | wind, pressure, temperature, sealevel, occupancy |
| value | DOUBLE PRECISION | Valor de la lectura |
| unit | VARCHAR(10) | Unidad de medida |
| scenario | VARCHAR(20) | Escenario activo |
| reading_time | TIMESTAMP | Timestamp del sensor |
| created_at | TIMESTAMP | Timestamp de insercion |

#### `complex_events` — Eventos complejos detectados

| Columna | Tipo | Descripcion |
|---|---|---|
| id | BIGSERIAL PK | ID autoincremental |
| pattern_id | VARCHAR(50) | ID del patron que lo detecto |
| pattern_name | VARCHAR(100) | Nombre del patron |
| alert_level | VARCHAR(10) | Nivel de alerta |
| beach_id | VARCHAR(50) | Playa afectada |
| details | JSONB | Detalles completos del evento |
| message | TEXT | Mensaje legible |
| detected_at | TIMESTAMP | Cuando se detecto |

### Consultas utiles

```sql
-- Ver ultimos eventos complejos
SELECT * FROM beach_monitor.complex_events ORDER BY detected_at DESC LIMIT 20;

-- Ver ultima alerta por playa
SELECT * FROM beach_monitor.latest_alerts;

-- Contar alertas por nivel
SELECT alert_level, COUNT(*) FROM beach_monitor.complex_events GROUP BY alert_level;

-- Ver patrones activos
SELECT pattern_id, name, alert_level, enabled FROM beach_monitor.esper_patterns;

-- Desactivar un patron sin tocar codigo Java
UPDATE beach_monitor.esper_patterns SET enabled = false WHERE pattern_id = 'heat_wave';

-- Contar lecturas por playa
SELECT beach_id, COUNT(*) FROM beach_monitor.sensor_readings GROUP BY beach_id;
```

---

## Topics MQTT

### Publicacion — Sensor Simulator → Mosquitto

Cada playa publica 5 topics individuales cada 3 segundos:

| Topic | Sensor | Unidad |
|---|---|---|
| `beach/{beachId}/wind` | Velocidad del viento | km/h |
| `beach/{beachId}/pressure` | Presion atmosferica | hPa |
| `beach/{beachId}/temperature` | Temperatura | °C |
| `beach/{beachId}/sealevel` | Nivel del mar | m |
| `beach/{beachId}/occupancy` | Ocupacion | % |

> Valores de `{beachId}`: `victoria`, `caleta`, `cortadura`

### Suscripcion — Node-RED Orchestrator ← Mosquitto

Node-RED se suscribe con wildcard `beach/#` — un unico nodo captura los 15 topics simultaneamente. Los agrupa por `beachId`, espera los 5 sensores y construye un `BeachCombinedEvent` JSON que envia a RabbitMQ.

```
beach/#
  ├── beach/victoria/wind
  ├── beach/victoria/pressure
  ...
  └── beach/cortadura/occupancy
```

### Publicacion — CEP Engine → Mosquitto (Alertas)

| Topic | Nivel |
|---|---|
| `alerts/{beachId}/YELLOW` | Alerta amarilla |
| `alerts/{beachId}/ORANGE` | Alerta naranja |
| `alerts/{beachId}/RED` | Alerta roja |

### Formato JSON — evento individual

```json
{
  "beachId": "victoria",
  "sensorType": "wind",
  "value": 45.67,
  "unit": "km/h",
  "timestamp": "2026-04-15T14:30:00Z",
  "scenario": "LEVANTE"
}
```

### Formato JSON — BeachCombinedEvent (orquestador → CEP)

```json
{
  "beachId": "victoria",
  "timestamp": "2026-04-15T14:30:01.123Z",
  "windSpeed": 45.67,
  "pressure": 1008.3,
  "temperature": 27.4,
  "seaLevel": 0.85,
  "occupancy": 72
}
```

---

## Configuracion Externa

| Fichero / Tabla | Que configura | Donde se usa |
|---|---|---|
| `config/sensors-config.json` | Playas, sensores, rangos, escenarios | Sensor Simulator |
| `config/event-types.json` | Tipos de eventos de Esper y sus propiedades | CEP Engine |
| `beach_monitor.esper_patterns` (PostgreSQL) | Sentencias EPL, niveles de alerta, estado activo | CEP Engine |

Para anadir un nuevo patron CEP: `INSERT` en `esper_patterns` y reiniciar el CEP Engine.  
Para anadir una nueva playa: agregar entrada en `sensors-config.json` y reiniciar el simulador.


## Extensibilidad, Escalabilidad, Interoperabilidad y ODS

### Extensibilidad
- Anadir una nueva playa: agregar entrada en `sensors-config.json`. Sin cambios en Java.
- Anadir un nuevo tipo de sensor: agregar en el JSON de configuracion y crear un nuevo simulador.
- Anadir un nuevo patron CEP: `INSERT` en `esper_patterns` y reiniciar el CEP Engine.
- Anadir un nuevo escenario: agregar en `sensors-config.json` y en el enum `WeatherScenario`.

### Escalabilidad
- Docker Compose permite escalar servicios: `docker-compose up --scale sensor-simulator=3`.
- RabbitMQ distribuye carga entre multiples consumidores de forma nativa.
- PostgreSQL soporta alto volumen de escrituras con connection pooling.
- Arquitectura event-driven y desacoplada por mensajeria.

### Interoperabilidad
- **MQTT** (ISO estándar) permite que cualquier dispositivo IoT se integre.
- **AMQP** (ISO/IEC 19464) para mensajeria fiable entre servicios.
- **REST API** para integracion con sistemas externos.
- **JSON** como formato universal de datos.

### Objetivos de Desarrollo Sostenible (ODS)
- **ODS 11 - Ciudades y comunidades sostenibles:** Monitoreo inteligente de infraestructura costera urbana.
- **ODS 13 - Accion por el clima:** Sistema de alerta temprana para eventos climaticos extremos.
- **ODS 14 - Vida submarina:** Control del nivel del mar protege ecosistemas marinos costeros.
- **ODS 3 - Salud y bienestar:** Alertas de ola de calor y control de ocupacion protegen la salud publica.
