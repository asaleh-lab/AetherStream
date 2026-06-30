# Architecture: AetherStream

**Status**: Complete | **Created**: 2026-06-05 | **Constitution**: v1.0.2

Reviewer entry point: [README.md](../../README.md) (how to run the demo, endpoints, observability).

This document defines the system architecture that realizes [spec.md](spec.md): module
boundaries, Kafka topics, Outbox design, CQRS flow, stream-processing topology, persistence,
APIs, and local Compose deployment.

AetherStream uses wind-energy telemetry as its scenario, but the layout—simulated feeds,
transactional outbox, Kafka backbone, Flink processing, CQRS read models, and a live
dashboard—is the same shape found in industrial IoT, manufacturing monitoring, and other
real-time telemetry pipelines.

## 1. Technology stack

| Concern | Choice |
|---|---|
| Language / runtime | Java 21 (Corretto) |
| Application framework | Spring Boot 3.3.x |
| Stream processing | Apache Flink 1.19 (Flink-style joins/windows/keyed state) |
| Messaging | Apache Kafka (KRaft mode, single broker for local) |
| Persistence | PostgreSQL 16 + Flyway migrations |
| Build | Maven multi-module reactor + committed Maven Wrapper |
| UI | .NET 10 Blazor Server + Radzen, over REST + WebSocket |
| Logging | Logback + logstash-logback-encoder (JSON) + MDC correlation id |
| Testing | JUnit 5 + Testcontainers (Kafka, PostgreSQL) |
| Local infra | `infra/docker-compose.yml` (full stack, single command) |
| Process | Spec-driven; abbreviated spec-kit path — see [HANDOFF.md](../../HANDOFF.md#development-process-spec-driven) |

## 2. Monorepo module layout

```text
AetherStream/
  pom.xml                       # parent reactor
  services/
    write-side/                 # Spring Boot: ingest REST APIs, CQRS, domain, outbox, PostgreSQL
    datasource/                 # Thin producer: turbine + grid simulators -> POST write-side
    outbox-relay/               # Spring Boot: polls outbox_events -> Kafka, retries, DLQ
    stream-processor/           # Flink: aggregation join + anomaly detection
    decision-engine/            # Flink/consumer: optimization recommendations
    api-gateway/                # Spring Boot: query APIs + WebSocket push
  core/
    domain/                     # pure model (no framework deps)
    application/                # command/query bus, handlers, ports
    infrastructure/             # JPA, Kafka, adapters, correlation, logging
  ui/
    blazor-dashboard/           # .NET 10 Blazor Server + Radzen
  infra/
    docker-compose.yml  docker/  observability/
  # Flyway migrations live on the infrastructure classpath:
  #   core/infrastructure/src/main/resources/db/migration/V*.sql
  scripts/                      # smoke-ingest.ps1 and dev helpers
  specs/                        # spec.md, architecture.md (no plan.md / tasks.md)
  HANDOFF.md                    # spec-driven continuity across sessions
  README.md
```

Dependency direction (Clean Architecture): `domain` <- `application` <- `infrastructure` <- services.

## 3. Component & data flow

```mermaid
flowchart LR
  turbineSim[Turbine simulator] --> datasource[datasource single producer]
  gridSim[Grid simulator] --> datasource

  datasource -->|HTTP POST| writeSide[write-side CQRS plus outbox]

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
| `turbine-events` | turbineId | turbine telemetry | write-side (via relay) | stream-processor |
| `grid-events` | region | grid load | write-side (via relay) | stream-processor |
| `energy-state-events` | region | aggregated energy state | stream-processor | api-gateway, decision-engine |
| `alerts` | region/turbineId | alert (type, severity) | stream-processor | api-gateway |
| `recommendations` | region | optimization suggestion | decision-engine | api-gateway |
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
| aggregate_type | text | e.g. `Turbine`, `GridLoad` |
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

- **Write side**: the **`write-side`** service exposes ingest REST APIs; the thin **`datasource`**
  service POSTs simulated/polled readings. Handlers in `core/application` (via `CommandBus`)
  validate domain rules, persist write-model state where applicable, and insert outbox rows
  in the same transaction.
- **Read side**: `api-gateway` serves queries via a `QueryBus`. Kafka projections update
  `energy_state_snapshot`, `alerts`, and `recommendations`. `GET /api/turbines/{id}` reads
  write-model `turbine_state` via `TurbineQueryPort` (shared-DB read at demo scale).
- Buses are thin interfaces (`CommandBus`, `QueryBus`, `CommandHandler<C>`,
  `QueryHandler<Q,R>`) — the MediatR-equivalent — keeping handlers decoupled.

## 7. Stream processing topology (Flink)

- **Aggregation job** (`stream-processor`): consume `turbine-events` and
  `grid-events`; key by region; window (e.g. tumbling/sliding) and join to compute
  `totalWindPower`, `gridDemand`, `efficiencyScore`; emit `energy-state-events`.
- **Anomaly job** (`stream-processor`): keyed rules over turbine and grid streams —
  vibration-spike detection, failure patterns, grid-overload risk -> `alerts`.
- **Decision job** (`decision-engine`): consume `energy-state-events`; apply optimization
  rules; emit recommendations / balancing signals.
- Late/out-of-order data handled with event-time watermarks and allowed lateness.

In an industrial or IoT context, the aggregation and anomaly jobs map directly to combining
sensor readings across assets, detecting threshold breaches, and triggering downstream
actions.

## 8. Persistence (read/write separation)

- Write model: `turbine_state` (+ `outbox_events`).
- Read models: `energy_state_snapshot`, `alerts`, `recommendations`. Updated by gateway-side
  consumers from Kafka. Schema owned by Flyway on the infrastructure classpath
  (`core/infrastructure/src/main/resources/db/migration/V*.sql`).

## 9. API & real-time gateway

- Command APIs (write, on **`write-side`**): `POST /api/ingest/turbine`,
  `/api/ingest/grid`. The **`datasource`** service forwards readings to these endpoints; it
  does not expose ingest APIs itself.
- Query APIs (read, on **`api-gateway`**): `GET /api/energy/latest`, `/api/alerts`,
  `/api/recommendations`, `/api/turbines/{id}`.
- WebSocket (`/ws/realtime`): gateway pushes `energy-state`, `alert`, `recommendation`, and
  `turbine` events to the Blazor UI.

## 10. Observability

- JSON logging via Logback + logstash-logback-encoder.
- Correlation id created at ingestion, stored in MDC, written to the outbox event envelope,
  propagated as a Kafka header, and restored into MDC by each consumer/stream operator.
- Spring Boot Actuator health + metrics on every Spring deployable; Flink jobs expose their
  own metrics.
- Local observability stack (included in compose): **Grafana** (UI), **Loki** (logs),
  **Promtail** (Docker log shipper), **Prometheus** (scrapes `/actuator/prometheus` on four
  Spring services under job `spring-services`). Config: `infra/observability/`. Pre-built
  dashboard and Grafana Explore deep links: repo [README](../../README.md#try-it-locally).

## 11. Local deployment

`infra/docker-compose.yml` brings up the full demo stack: Kafka (KRaft), PostgreSQL 16,
Kafka UI, **`write-side`** (CQRS + outbox + DB), **`datasource`** (turbine/grid
simulators), **`outbox-relay`**, **`stream-processor`** (Flink aggregation + anomaly detection),
**`decision-engine`**, **`api-gateway`** (read-model projections, query APIs, WebSocket),
**`blazor-dashboard`**, and **Grafana + Loki + Prometheus + Promtail** (logs and metrics UI on
port 3000). A one-shot `kafka-init` container applies topic creation (`create-topics.sh`) and
exits 0. Flyway runs on write-side startup.

```powershell
docker compose -f infra/docker-compose.yml up -d --build
```

| Container | Port | Role |
|-----------|------|------|
| `aether-postgres` | 5432 | Database |
| `aether-kafka` | 9094 (host) | Event backbone |
| `aether-kafka-ui` | 8089 | Topic browser |
| `aether-write-side` | 8080 | Ingest APIs, CQRS, outbox, PostgreSQL |
| `aether-datasource` | 8081 | External feed simulator (POSTs to write-side) |
| `aether-outbox-relay` | 8084 | Outbox → Kafka relay |
| `aether-stream-processor` | — | Flink aggregation + anomaly detection |
| `aether-decision-engine` | — | Flink optimization recommendations |
| `aether-api-gateway` | 8085 | Query APIs + WebSocket push |
| `aether-blazor-dashboard` | 8086 | Blazor + Radzen UI |
| `aether-grafana` | 3000 | Logs + metrics UI |
| `aether-prometheus` | 9090 | Metrics scraper |
| `aether-loki` | 3100 | Log store |

Grafana local compose login: `admin` / `admin`. Promtail collects stdout from
`aether-*` containers; Java services emit JSON logs with `correlationId` for traceability in
Loki/Explore.

**Docker build context:** `.dockerignore` excludes `ui/blazor-dashboard/bin` and `obj` but must
include `ui/blazor-dashboard/` sources for the dashboard image. Flink shaded modules
(`stream-processor`, `decision-engine`) declare `flink-connector-base` explicitly because
`flink-connector-kafka` lists it as `provided` (intended for Flink cluster classpath, not
standalone fat jars).

## 12. Key decisions & trade-offs

- **At-least-once + idempotent consumers** instead of end-to-end Kafka exactly-once: simpler,
  robust, and the idiomatic way the Outbox pattern is taught and used.
- **Maven multi-module** over Gradle: clear module boundaries; Flink jobs are separate shaded
  modules to avoid dependency clashes with Spring Boot.
- **Blazor Server** over WASM: straightforward path for high-frequency real-time push.
- **Single-node Kafka/Postgres**: appropriate for a local demo; the topology generalizes to
  clustered deployments when scale requires it.
- **Simulated data sources**: keeps the demo self-contained; swapping in real MQTT, OPC-UA, or
  HTTP device feeds would plug into the same write-side ingest APIs.
