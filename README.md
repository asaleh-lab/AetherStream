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

- **Kafka event-driven architecture** — two ingestion streams (turbine telemetry, grid load) converge on a topic backbone; downstream processing is decoupled from producers.
- **CQRS** — write model (commands, domain state, outbox) separated from read model (query-optimized projections served by the API gateway).
- **Outbox pattern** — no dual-write: domain changes and `outbox_events` rows commit in one transaction; a relay publishes to Kafka with at-least-once delivery and idempotent consumers.
- **Stream processing (Flink-style)** — aggregation joins, anomaly detection, and a decision engine produce energy state, alerts, and recommendations.
- **Real-time UI** — Blazor Server + Radzen dashboard consuming REST and WebSocket from the gateway.

## Two-part layout

The repo deliberately separates **producers** from the **backbone** so the demo story is easy to follow:

| Part | Modules | Responsibility |
|------|---------|----------------|
| **Data source** | `datasource` | One thin Spring Boot app simulating the outside world. No DB, domain, or CQRS. Two schedulers at real-world cadences POST JSON to write-side: turbine telemetry (5s), grid load (15s). |
| **Backbone** | `core/*`, `write-side`, `outbox-relay`, Flink jobs, `api-gateway` | `application.yml`, JSON logging, CQRS, domain models, PostgreSQL, outbox, Kafka relay, stream processing, query APIs. |

```text
datasource  --HTTP POST-->  write-side  --outbox-->  relay  -->  Kafka  -->  Flink  -->  api-gateway  -->  UI
```

## Repository layout

```text
core/           domain, application (CQRS bus), infrastructure (JPA, Kafka, Flyway)
services/       write-side, datasource, outbox-relay, api-gateway, stream-processor, decision-engine
ui/             blazor-dashboard (.NET 10 + Radzen)
infra/          docker-compose, Dockerfiles, observability configs (Grafana/Loki/Prometheus)
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

**Backend pipeline** (default compose):

```powershell
docker compose -f infra/docker-compose.yml up -d --build
```

**Full demo including Blazor UI**:

```powershell
docker compose -f infra/docker-compose.yml --profile full up -d --build
```

**Observability stack** (Grafana + Loki + Prometheus — free, open source):

```powershell
docker compose -f infra/docker-compose.yml --profile observability up -d --build
```

**Full demo with UI and observability**:

```powershell
docker compose -f infra/docker-compose.yml --profile full --profile observability up -d --build
```

Brings up Postgres, Kafka, Kafka UI, **write-side** (CQRS + outbox), **datasource**
(auto-forwarding turbine and grid readings), **outbox-relay**, **stream-processor**
(Flink aggregation + anomaly detection), **decision-engine** (optimization recommendations),
and **api-gateway** (query APIs + WebSocket). With `--profile full`, also starts
**blazor-dashboard** on port 8086. Flyway runs on service startup.

| Container | Role | Port |
|-----------|------|------|
| `aether-postgres` | Database | 5432 |
| `aether-kafka` | Event backbone (host) | 9094 |
| `aether-kafka-ui` | Topic browser | 8089 |
| `aether-write-side` | CQRS ingest + outbox + DB | 8080 |
| `aether-datasource` | External feed simulator | 8081 |
| `aether-outbox-relay` | Outbox → Kafka relay | 8084 |
| `aether-stream-processor` | Flink job (no HTTP port) | — |
| `aether-decision-engine` | Flink job: optimization recommendations (no HTTP port) | — |
| `aether-api-gateway` | Query APIs + WebSocket | 8085 |
| `aether-blazor-dashboard` | Blazor + Radzen UI (`--profile full`) | 8086 |
| `aether-grafana` | Logs + metrics UI (`--profile observability`) | 3000 |
| `aether-prometheus` | Metrics scraper (`--profile observability`) | 9090 |
| `aether-loki` | Log store (`--profile observability`) | 3100 |

### Observability (optional `--profile observability`)

Open-source stack for log search and service metrics — no license cost when self-hosted:

| Tool | Role |
|------|------|
| **Grafana** | Web UI for dashboards and log search |
| **Loki** | Collects and stores container stdout (JSON logs from Java services) |
| **Promtail** | Ships Docker container logs to Loki |
| **Prometheus** | Scrapes Spring Boot `/actuator/prometheus` every 15s |

**Grafana:** `http://localhost:3000` — login `admin` / `aether` (local demo only).

Open the pre-built dashboard: **Dashboards → AetherStream → AetherStream Logs**.

**Explore logs (Grafana → Explore → Loki)** — start with a broad query, then narrow down:

```logql
# Start here — all application containers (should always show lines)
{container=~"aether-datasource|aether-write-side|aether-outbox-relay|aether-api-gateway"}

# Datasource simulator (most active — logs every 5–15s)
{container="aether-datasource"}

# Write-side ingest with correlation id (after rebuild with IngestAccessLogFilter)
{container="aether-write-side"} | json | correlationId != ""
```

Set the time range to **Last 15 minutes** if the view looks empty.

**Troubleshooting empty Loki results:** metrics and logs are separate systems. If Prometheus
has data but Explore looks empty, you are usually filtering too aggressively (e.g. the
`correlationId` filter before ingest logging existed) or the time range is too narrow.
Try `{container="aether-datasource"}` first — it logs continuously.

**Explore metrics (Grafana → Explore → Prometheus):**

```promql
rate(http_server_requests_seconds_count{job="spring-services"}[1m])
jvm_memory_used_bytes{job="spring-services"}
```

Prometheus targets UI: `http://localhost:9090/targets` (all four Spring services should be **UP**).

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

**Query APIs:** `GET http://localhost:8085/api/energy/latest`, `/api/alerts`, `/api/recommendations`, `/api/turbines/{id}`  
**WebSocket:** `ws://localhost:8085/ws/realtime`  
**Blazor UI (compose):** `http://localhost:8086`  
**Blazor UI (local dev):** `dotnet run --project ui/blazor-dashboard` (default port 5000)

First `docker compose up --build` is slow (Maven and .NET builds inside images); later runs
use the layer cache. On Windows PowerShell, chain commands with `;` not `&&`.

Check container health:

```powershell
docker compose -f infra/docker-compose.yml --profile full ps -a
```

## Status

Feature-complete for the portfolio demo. All six delivery phases are done: write-side + outbox,
relay, Flink stream processing (aggregation, anomaly detection, decision-engine recommendations),
API gateway, Blazor dashboard, and optional observability profile — all in docker-compose.
Track session state in [HANDOFF.md](HANDOFF.md).

## License

Portfolio / demonstration use. No production warranty implied.
