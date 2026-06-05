# AetherStream

A portfolio project: a real-time wind-energy monitoring platform built to demonstrate
production-shaped design of an event-driven streaming system. The backend is JVM-native
(Java 21, Spring Boot, Apache Flink, Kafka, PostgreSQL); the UI is .NET 10 Blazor with
Radzen. The brief's ASP.NET-to-JVM mapping table is documentation intent — this is not a
literal .NET port, but a system that shows I can design and implement the same patterns
in the Java ecosystem quickly.

Development followed a spec-driven workflow (Cursor + [spec-kit](https://github.com/github/spec-kit)):
constitution, functional spec, and architecture were written before scaffolding code.
See [HANDOFF.md](HANDOFF.md) for cross-session state and [specs/001-aetherstream/](specs/001-aetherstream/) for the design artifacts.

## What it demonstrates

- **Kafka event-driven architecture** — three ingestion streams (weather, turbine telemetry, grid load) converge on a topic backbone; downstream processing is decoupled from producers.
- **CQRS** — write model (commands, domain state, outbox) separated from read model (query-optimized projections served by the API gateway).
- **Outbox pattern** — no dual-write: domain changes and `outbox_events` rows commit in one transaction; a relay publishes to Kafka with at-least-once delivery and idempotent consumers.
- **Stream processing (Flink-style)** — aggregation joins, anomaly detection, and a decision engine produce energy state, alerts, and recommendations.
- **Real-time UI** — Blazor Server + Radzen dashboard consuming REST and WebSocket from the gateway.

## Two-part layout

The repo deliberately separates **producers** from the **backbone** so the demo story is easy to follow:

| Part | Modules | Responsibility |
|------|---------|----------------|
| **Data source** | `datasource` | One thin Spring Boot app simulating the outside world. No DB, domain, or CQRS. Three schedulers at real-world cadences POST JSON to write-side: weather **GET poll** (60s), turbine telemetry (5s), grid load (15s). |
| **Backbone** | `core/*`, `write-side`, `outbox-relay`, Flink jobs, `api-gateway` | `application.yml`, JSON logging, CQRS, domain models, PostgreSQL, outbox, Kafka relay, stream processing, query APIs. |

```text
datasource  --HTTP POST-->  write-side  --outbox-->  relay  -->  Kafka  -->  Flink  -->  api-gateway  -->  UI
```

## Repository layout

```text
core/           domain, application (CQRS bus), infrastructure (JPA, Kafka, Flyway)
services/       write-side, datasource, outbox-relay, api-gateway, stream-processor, decision-engine
ui/             blazor-dashboard (.NET 10 + Radzen)
infra/          docker-compose, Dockerfiles (Kafka KRaft, PostgreSQL, write-side, datasource)
scripts/        smoke-ingest.ps1 and other dev helpers
specs/          spec-kit artifacts
```

## Prerequisites

- **Reviewers / demo:** Docker only
- **Java development:** JDK 21 (Maven via `./mvnw`)
- **UI development:** .NET 10 SDK

## Build

```powershell
# Java backend (all modules)
.\mvnw.cmd -DskipTests package

# Blazor UI
dotnet build ui/blazor-dashboard
```

## Local stack (plug-and-play)

```powershell
docker compose -f infra/docker-compose.yml up -d --build
```

Brings up Postgres, Kafka, Kafka UI, **write-side** (CQRS + outbox), **datasource**
(auto-forwarding weather, turbine, and grid readings), **outbox-relay**, **stream-processor**,
and **api-gateway** (query APIs + WebSocket). Flyway runs on service startup.

| Container | Role | Port |
|-----------|------|------|
| `aether-postgres` | Database | 5432 |
| `aether-kafka` | Event backbone (host) | 9094 |
| `aether-kafka-ui` | Topic browser | 8089 |
| `aether-write-side` | CQRS ingest + outbox + DB | 8080 |
| `aether-datasource` | External feed simulator | 8081 |
| `aether-outbox-relay` | Outbox → Kafka relay | 8084 |
| `aether-api-gateway` | Query APIs + WebSocket | 8085 |

Smoke-test write-side ingest endpoints:

```powershell
.\scripts\smoke-ingest.ps1 -SkipCommit
```

Or POST manually (example — turbine telemetry):

```powershell
Invoke-RestMethod -Method POST -Uri http://localhost:8080/api/ingest/turbine `
  -ContentType application/json `
  -Body '{"turbineId":"T-001","rpm":12.5,"powerOutput":1500,"vibrationLevel":0.4}'
```

Expect HTTP 202 with `eventId`, `correlationId`, and `status: PENDING` (outbox row written).

Host bootstrap for Kafka is `localhost:9094`; containers use `kafka:9092`.

## Other services (not yet in compose)

| Service | Port |
|---------|------|
| Blazor dashboard | 5000 |

Query APIs: `GET http://localhost:8085/api/energy/latest`, `/api/alerts`, `/api/turbines/{id}`  
WebSocket: `ws://localhost:8085/ws/realtime`

## Status

**Phase 5 (query side + api-gateway)** is complete: read-model Kafka consumers, query REST
APIs, WebSocket push, and `api-gateway` in docker-compose. **Phase 6** (Blazor UI) is next.
Track progress in [HANDOFF.md](HANDOFF.md).

## License

Portfolio / demonstration use. No production warranty implied.
