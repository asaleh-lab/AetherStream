# AetherStream

A real-time wind-energy monitoring platform demonstrating production-shaped event-driven
streaming. JVM backend (Java 21, Spring Boot, Apache Flink, Kafka, PostgreSQL); .NET 10
Blazor + Radzen UI over REST and WebSocket.

## How it was built (spec-kit + AI)

This project was implemented in a **short timeframe using AI-assisted development** (Cursor).
To keep scope coherent and reviewable under that constraint, development followed a
**spec-driven workflow** with [spec-kit](https://github.com/github/spec-kit):

1. **Constitution** — non-negotiable principles ([.specify/memory/constitution.md](.specify/memory/constitution.md))
2. **Functional spec** — user stories and acceptance criteria ([specs/001-aetherstream/spec.md](specs/001-aetherstream/spec.md))
3. **Architecture** — components, data flow, deployment ([specs/001-aetherstream/architecture.md](specs/001-aetherstream/architecture.md))
4. **Implementation** — code generated and refined against those artifacts

The specs are the source of truth: they constrain AI output, make design intent auditable,
and let a reviewer evaluate *what* was built separately from *how fast* it was built.
Full artifact tree: [specs/001-aetherstream/](specs/001-aetherstream/).

## What it demonstrates

- **Kafka event-driven architecture** — turbine telemetry and grid load converge on a topic backbone; downstream processing is decoupled from producers.
- **CQRS** — write model (commands, domain state, outbox) separated from read model (query-optimized projections served by the API gateway).
- **Outbox pattern** — domain changes and `outbox_events` rows commit in one transaction; a relay publishes to Kafka with at-least-once delivery and idempotent consumers.
- **Stream processing (Flink-style)** — aggregation joins, anomaly detection, and a decision engine produce energy state, alerts, and recommendations.
- **Real-time UI** — Blazor Server + Radzen dashboard consuming REST and WebSocket from the gateway.
- **Observability** — Grafana, Loki, Promtail, and Prometheus for logs and metrics (local Docker Compose and Azure AKS).

## Architecture

Producers are separated from the backbone so the data flow is easy to follow:

| Part | Modules | Responsibility |
|------|---------|----------------|
| **Data source** | `datasource` | Thin Spring Boot app simulating external feeds. Two schedulers POST JSON to write-side: turbine telemetry (5s), grid load (15s). |
| **Backbone** | `core/*`, `write-side`, `outbox-relay`, Flink jobs, `api-gateway` | CQRS, domain models, PostgreSQL, outbox, Kafka relay, stream processing, query APIs. |

```text
datasource  --HTTP POST-->  write-side  --outbox-->  relay  -->  Kafka  -->  Flink  -->  api-gateway  -->  UI
```

```text
core/           domain, application (CQRS bus), infrastructure (JPA, Kafka, Flyway)
services/       write-side, datasource, outbox-relay, api-gateway, stream-processor, decision-engine
ui/             blazor-dashboard (.NET 10 + Radzen)
infra/          docker-compose, Dockerfiles, observability; Terraform + K8s for Azure
scripts/        smoke-ingest.ps1 and other dev helpers
specs/          functional spec, architecture, tasks
```

## Prerequisites

- **Reviewers / demo:** Docker only
- **Java development:** JDK 21 (Maven via `./mvnw`)
- **UI development:** .NET 10 SDK

## Local demo

One command starts the full stack — backend, Blazor UI, and observability:

```powershell
docker compose -f infra/docker-compose.yml up -d --build
```

First run is slow (Maven and .NET image builds); later runs use the layer cache.

| Endpoint | URL |
|----------|-----|
| Blazor dashboard | http://localhost:8086 |
| API gateway (REST) | http://localhost:8085/api/energy/latest |
| API gateway (WebSocket) | ws://localhost:8085/ws/realtime |
| Write-side ingest | http://localhost:8080/api/ingest/turbine |
| Kafka UI | http://localhost:8089 |
| Grafana | http://localhost:3000 (`admin` / `admin`) |
| Prometheus targets | http://localhost:9090/targets |

| Container | Role | Port |
|-----------|------|------|
| `aether-postgres` | Database | 5432 |
| `aether-kafka` | Event backbone (host) | 9094 |
| `aether-kafka-ui` | Topic browser | 8089 |
| `aether-write-side` | CQRS ingest + outbox + DB | 8080 |
| `aether-datasource` | External feed simulator | 8081 |
| `aether-outbox-relay` | Outbox → Kafka relay | 8084 |
| `aether-stream-processor` | Flink aggregation + anomaly detection | — |
| `aether-decision-engine` | Flink optimization recommendations | — |
| `aether-api-gateway` | Query APIs + WebSocket | 8085 |
| `aether-blazor-dashboard` | Blazor + Radzen UI | 8086 |
| `aether-grafana` | Logs + metrics UI | 3000 |
| `aether-prometheus` | Metrics scraper | 9090 |
| `aether-loki` | Log store | 3100 |

Check health: `docker compose -f infra/docker-compose.yml ps -a`

### Observability

Pre-built dashboard: **Dashboards → AetherStream → AetherStream Logs**.

**Quick Explore links** (pre-filled queries, last 15 minutes):

| What you'll see | Link |
|-----------------|------|
| Datasource simulator — continuous turbine/grid POSTs every 5–15s | [Open in Grafana → Loki](http://localhost:3000/explore?orgId=1&schemaVersion=1&panes=%7B%22ds%22%3A%7B%22datasource%22%3A%22loki%22%2C%22range%22%3A%7B%22to%22%3A%22now%22%2C%22from%22%3A%22now-15m%22%7D%2C%22queries%22%3A%5B%7B%22datasource%22%3A%7B%22uid%22%3A%22loki%22%2C%22type%22%3A%22loki%22%7D%2C%22expr%22%3A%22%7Bcontainer%3D%5C%22aether-datasource%5C%22%7D%22%2C%22refId%22%3A%22A%22%7D%5D%7D%7D) |
| Write-side ingest with `correlationId` — trace a request into the outbox | [Open in Grafana → Loki](http://localhost:3000/explore?orgId=1&schemaVersion=1&panes=%7B%22ws%22%3A%7B%22datasource%22%3A%22loki%22%2C%22range%22%3A%7B%22to%22%3A%22now%22%2C%22from%22%3A%22now-15m%22%7D%2C%22queries%22%3A%5B%7B%22datasource%22%3A%7B%22uid%22%3A%22loki%22%2C%22type%22%3A%22loki%22%7D%2C%22expr%22%3A%22%7Bcontainer%3D%5C%22aether-write-side%5C%22%7D%20%7C%20json%20%7C%20correlationId%20!%3D%20%5C%22%5C%22%22%2C%22refId%22%3A%22A%22%7D%5D%7D%7D) |
| HTTP request rate across Spring services | [Open in Grafana → Prometheus](http://localhost:3000/explore?orgId=1&schemaVersion=1&panes=%7B%22pm%22%3A%7B%22datasource%22%3A%22prometheus%22%2C%22range%22%3A%7B%22to%22%3A%22now%22%2C%22from%22%3A%22now-15m%22%7D%2C%22queries%22%3A%5B%7B%22datasource%22%3A%7B%22uid%22%3A%22prometheus%22%2C%22type%22%3A%22prometheus%22%7D%2C%22expr%22%3A%22rate(http_server_requests_seconds_count%7Bjob%3D%5C%22spring-services%5C%22%7D%5B1m%5D)%22%2C%22refId%22%3A%22A%22%7D%5D%7D%7D) |

The same LogQL works locally and on Azure AKS (`container` labels match).

### Smoke test

```powershell
.\scripts\smoke-ingest.ps1 -SkipCommit
```

Manual ingest example (expect HTTP 202 with `eventId`, `correlationId`, `status: PENDING`):

```powershell
Invoke-RestMethod -Method POST -Uri http://localhost:8080/api/ingest/turbine `
  -ContentType application/json `
  -Body '{"turbineId":"T-001","rpm":12.5,"powerOutput":1500,"vibrationLevel":0.4}'
```

## Build (from source)

```powershell
.\mvnw.cmd -DskipTests package          # Java backend
dotnet build ui/blazor-dashboard        # Blazor UI
dotnet run --project ui/blazor-dashboard  # UI dev server (default port 5000)
```

## Azure deployment

The same stack runs on Azure AKS. Runbook: [infra/terraform/README.md](infra/terraform/README.md).

**Live demo URLs and credentials are in the motivation letter** — not published in this repo.

Architecture diagram and deploy steps: [infra/terraform/README.md — Architecture](infra/terraform/README.md#architecture).

Blazor and Grafana are deployed **inside the AKS cluster** (public LoadBalancer Services), not on
App Service or separate VMs — a cost-driven placement choice. Backend services (api-gateway, Kafka,
Flink, write-side) use internal LoadBalancers. Grafana on AKS runs the same Loki + Prometheus
stack as local Docker Compose.

### Omitted for price consideration

- Application Gateway and WAF
- Private endpoints (PostgreSQL, ACR, Key Vault)
- Hub-spoke VNet peering
- Premium ACR / private-link registry
- Multi-node AKS and zone redundancy
- App Service or standalone VMs for UI and Grafana — **Blazor and Grafana run in the AKS cluster** instead

## License

Portfolio / demonstration use. No production warranty implied.
