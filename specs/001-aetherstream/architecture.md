# Architecture: AetherStream

**Status**: Draft | **Created**: 2026-06-05 | **Constitution**: v1.0.0

This document defines the system architecture that realizes [spec.md](spec.md). It is the
authoritative reference for module boundaries, Kafka topics, the Outbox design, CQRS flow,
the stream-processing topology, persistence, APIs, and observability.

## 1. Technology stack

| Concern | Choice |
|---|---|
| Language / runtime | Java 21 (Corretto) |
| Application framework | Spring Boot 3.3.x |
| Stream processing | Apache Flink 1.19 (Flink-style joins/windows/keyed state) |
| Messaging | Apache Kafka (KRaft mode, single broker for local) |
| Persistence | PostgreSQL 16 + Flyway migrations |
| Build | Maven multi-module reactor + committed Maven Wrapper |
| UI | .NET 8 Blazor Server + Radzen, over REST + WebSocket |
| Logging | Logback + logstash-logback-encoder (JSON) + MDC correlation id |
| Testing | JUnit 5 + Testcontainers (Kafka, PostgreSQL) |
| Local infra | docker-compose |

ASP.NET-to-JVM mapping from the brief (documentation intent only): `BackgroundService` ->
Kafka consumer / Flink job; `MediatR` -> command/query bus; `EF Core` -> JPA/Hibernate;
`SignalR` -> WebSocket gateway; `Serilog` -> Logback JSON + MDC; `Hangfire` -> Flink jobs;
`appsettings.json` -> `application.yml`; Transactional Outbox -> outbox table + relay.

## 2. Monorepo module layout

```text
AetherStream/
  pom.xml                       # parent reactor
  services/
    write-side/                 # Spring Boot: ingest REST APIs, CQRS, domain, outbox, PostgreSQL
    datasource-weather/         # Thin producer: GET poll weather API -> POST write-side
    datasource-turbine/         # Thin producer: simulated turbine telemetry -> POST write-side
    datasource-grid/            # Thin producer: simulated grid feed -> POST write-side
    outbox-relay/               # Spring Boot: polls outbox_events -> Kafka, retries, DLQ
    stream-processor/           # Flink: aggregation join + anomaly detection
    decision-engine/            # Flink/consumer: optimization recommendations
    api-gateway/                # Spring Boot: query APIs + WebSocket push
  core/
    domain/                     # pure model (no framework deps)
    application/                # command/query bus, handlers, ports
    infrastructure/             # JPA, Kafka, adapters, correlation, logging
  ui/
    blazor-dashboard/           # .NET 8 Blazor Server + Radzen
  infra/
    docker-compose.yml  kafka/create-topics.sh  postgres/
  # Flyway migrations live on the infrastructure classpath:
  #   core/infrastructure/src/main/resources/db/migration/V*.sql
  specs/                        # spec-kit artifacts
  HANDOFF.md  README.md
```

Dependency direction (Clean Architecture): `domain` <- `application` <- `infrastructure` <- services.

## 3. Component & data flow

```mermaid
flowchart LR
  weatherApi[Weather API] --> dsWeather[datasource-weather]
  turbineSim[Turbine simulator] --> dsTurbine[datasource-turbine]
  gridSim[Grid simulator] --> dsGrid[datasource-grid]

  dsWeather -->|HTTP POST| writeSide[write-side CQRS plus outbox]
  dsTurbine -->|HTTP POST| writeSide
  dsGrid -->|HTTP POST| writeSide

  writeSide --> db[(PostgreSQL write model plus outbox)]

  db --> relay[outbox-relay]
  relay --> topics{{Kafka topics}}

  topics --> processor[stream-processor Flink]
  processor --> topics
  processor --> decision[decision-engine]
  decision --> topics

  topics --> gateway[api-gateway]
  read[(PostgreSQL read models)] --> gateway
  gateway -->|WebSocket| ui[Blazor plus Radzen UI]
  gateway -->|REST| ui
```

## 4. Kafka topic design

| Topic | Key | Payload (summary) | Producer | Consumers |
|---|---|---|---|---|
| `weather-events` | region | weather reading | write-side (via relay) | stream-processor |
| `turbine-events` | turbineId | turbine telemetry | write-side (via relay) | stream-processor |
| `grid-events` | region | grid load | write-side (via relay) | stream-processor |
| `energy-state-events` | region | aggregated energy state | stream-processor | api-gateway, decision-engine |
| `alerts` | region/turbineId | alert (type, severity) | stream-processor | api-gateway |
| `dead-letter-events` | original key | failed event envelope | outbox-relay, consumers | ops/inspection |
| `outbox-events` | aggregateId | (reserved) relay/CDC channel | outbox-relay | n/a (table-driven in v1) |

Conventions: events are versioned JSON envelopes carrying `eventId`, `eventType`,
`occurredAt`, `correlationId`, and `payload`. Keys are chosen so per-entity ordering holds
within a partition. Local default: 1 partition, replication factor 1 (demo).

## 5. Outbox pattern (reliability core)

### Write-side transaction

```mermaid
sequenceDiagram
  participant API as Write-side ingest API
  participant H as Command Handler
  participant DB as PostgreSQL
  participant R as Outbox Relay
  participant K as Kafka
  API->>H: command (with correlationId)
  H->>DB: BEGIN
  H->>DB: upsert domain state (write model)
  H->>DB: insert outbox_events (PENDING)
  H->>DB: COMMIT
  Note over H,K: No Kafka publish inside the transaction
  R->>DB: poll PENDING (batch, FOR UPDATE SKIP LOCKED)
  R->>K: publish event (idempotent, keyed)
  R->>DB: mark SENT (processed_at)
  alt publish fails after retries
    R->>K: route to dead-letter-events
    R->>DB: mark FAILED
  end
```

### `outbox_events` table

| Column | Type | Notes |
|---|---|---|
| id | UUID PK | also used as `eventId` for downstream dedupe |
| aggregate_type | text | e.g. `Turbine`, `GridLoad`, `WeatherReading` |
| aggregate_id | text | entity id |
| event_type | text | e.g. `TurbineTelemetryRecorded` |
| payload | jsonb | event body |
| status | text | `PENDING` \| `SENT` \| `FAILED` |
| created_at | timestamptz | default now() |
| processed_at | timestamptz | set when SENT/FAILED |

Indexes: partial index on `status = 'PENDING'` ordered by `created_at` for efficient polling.

### Relay algorithm

1. Poll a batch of `PENDING` rows oldest-first using `FOR UPDATE SKIP LOCKED` (safe for
   multiple relay instances).
2. Map `aggregate_type`/`event_type` to the destination topic; publish keyed by `aggregate_id`.
3. On success, mark `SENT` with `processed_at`. On transient failure, leave `PENDING` for
   retry with backoff; on exhausted retries, publish to `dead-letter-events` and mark `FAILED`.
4. At-least-once delivery; downstream dedupes by `eventId` (the outbox `id`).

## 6. CQRS

- **Write side**: ingestion services receive commands (REST or simulated), validate domain
  rules in `core/application` handlers dispatched via a `CommandBus`, persist write-model
  state, and insert outbox rows in the same transaction.
- **Read side**: `api-gateway` serves queries via a `QueryBus` against read-model
  projections (`energy_state_snapshot`, `alerts`, `turbine_state`) updated from Kafka.
- Buses are thin interfaces (`CommandBus`, `QueryBus`, `CommandHandler<C>`,
  `QueryHandler<Q,R>`) — the MediatR-equivalent — keeping handlers decoupled.

## 7. Stream processing topology (Flink)

- **Aggregation job** (`stream-processor`): consume `turbine-events`, `weather-events`,
  `grid-events`; key by region; window (e.g. tumbling/sliding) and join to compute
  `totalWindPower`, `gridDemand`, `efficiencyScore`; emit `energy-state-events`.
- **Anomaly job** (`stream-processor`): keyed rules over turbine and grid streams —
  vibration-spike detection, failure patterns, grid-overload risk -> `alerts`.
- **Decision job** (`decision-engine`): consume `energy-state-events`; apply optimization
  rules; emit recommendations / balancing signals.
- Late/out-of-order data handled with event-time watermarks and allowed lateness.

## 8. Persistence (read/write separation)

- Write model: `turbine_state` (+ `outbox_events`).
- Read models: `energy_state_snapshot`, `alerts`. Updated by gateway-side consumers from
  Kafka. Schema owned by Flyway, stored on the infrastructure classpath at
  `core/infrastructure/src/main/resources/db/migration/V1__init.sql` (so every service that
  depends on `infrastructure` applies it automatically on startup).

## 9. API & real-time gateway

- Command APIs (write): `POST /api/ingest/weather`, `/api/ingest/turbine`, `/api/ingest/grid`.
- Query APIs (read): `GET /api/energy/latest`, `/api/alerts`, `/api/turbines/{id}`.
- WebSocket: gateway pushes energy-state updates and alerts to the Blazor UI.

## 10. Observability

- JSON logging via Logback + logstash-logback-encoder.
- Correlation id created at ingestion, stored in MDC, written to the outbox event envelope,
  propagated as a Kafka header, and restored into MDC by each consumer/stream operator.
- Spring Boot Actuator health + metrics on every Spring deployable; Flink jobs expose their
  own metrics.

## 11. Local deployment

`infra/docker-compose.yml` brings up Kafka (KRaft), PostgreSQL 16, and a Kafka UI; a
one-shot init applies topic creation (`create-topics.sh`). Flyway runs on service startup.

## 12. Phased delivery

1. **Infra & skeleton** (current): monorepo, build files, domain model, topics + schema, docker-compose, spec-kit + git + handoff.
2. Write side + Outbox (command handlers, domain persistence, transactional outbox writes).
3. Outbox relay (idempotent batched publish, retries, DLQ).
4. Stream processing (aggregation join, anomaly detection, decision engine).
5. Query side + real-time gateway (read-model projections, query APIs, WebSocket push).
6. Blazor UI live + Testcontainers tests + correlation-id propagation + metrics.

## 13. Key decisions & trade-offs

- **At-least-once + idempotent consumers** instead of end-to-end Kafka exactly-once: simpler,
  robust, and the idiomatic way the Outbox pattern is taught and used.
- **Maven multi-module** over Gradle: most universally recognizable for a Java portfolio;
  Flink jobs are separate shaded modules to avoid dependency clashes with Spring Boot.
- **Blazor Server** over WASM: simplest path for high-frequency real-time push.
- **Single-node Kafka/Postgres**: demonstration scope; topology generalizes to clusters.
