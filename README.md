# AetherStream

A portfolio project: a real-time wind-energy monitoring platform built to demonstrate
production-shaped design of an event-driven streaming system. The backend is JVM-native
(Java 21, Spring Boot, Apache Flink, Kafka, PostgreSQL); the UI is .NET 8 Blazor with
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

## Repository layout

```text
core/           domain, application (CQRS bus), infrastructure (JPA, Kafka, Flyway)
services/       ingestion-*, outbox-relay, api-gateway, stream-processor, decision-engine
ui/             blazor-dashboard (.NET 8 + Radzen)
infra/          docker-compose (Kafka KRaft, PostgreSQL, kafka-ui)
specs/          spec-kit artifacts
```

## Prerequisites

- JDK 21, Docker, .NET 8 SDK
- Maven is optional — the repo includes `./mvnw`

## Build

```powershell
# Java backend (all modules)
.\mvnw.cmd -DskipTests package

# Blazor UI
dotnet build ui/blazor-dashboard
```

## Local infrastructure

```powershell
docker compose -f infra/docker-compose.yml up -d
```

- PostgreSQL: `localhost:5432` (db `aetherstream`, user/pass `aether`)
- Kafka: `localhost:9092`
- Kafka UI: http://localhost:8089

Flyway migrations run from the `infrastructure` module when Spring services start.

## Service ports (skeleton defaults)

| Service            | Port |
|--------------------|------|
| api-gateway        | 8080 |
| ingestion-weather  | 8081 |
| ingestion-turbine  | 8082 |
| ingestion-grid     | 8083 |
| outbox-relay       | 8084 |
| Blazor dashboard   | 5000 (launchSettings) |

## Status

Phase 1 (infra & skeleton) scaffolds the monorepo, schema, topics, docker-compose, and UI
shell. Business logic (command handlers, outbox relay, Flink topology, live WebSocket) lands
in Phases 2–6. Track progress in [HANDOFF.md](HANDOFF.md) and [PR #1](https://github.com/asaleh-lab/AetherStream/pull/1).

## License

Portfolio / demonstration use. No production warranty implied.
