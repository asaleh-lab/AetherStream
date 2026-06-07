# AetherStream

A real-time wind-energy monitoring platform demonstrating production-shaped event-driven
streaming: Kafka, CQRS, Outbox pattern, Flink-style processing, Blazor UI, and full
observability. **This README is for reviewers** — Docker is the only prerequisite.

Contributors: [HANDOFF.md](HANDOFF.md) (process, build from source, deployment notes).

## What it demonstrates

- **Event-driven backbone** — turbine telemetry and grid load on Kafka; producers decoupled from processing
- **CQRS** — write model (commands, outbox) vs read model (query APIs, projections)
- **Outbox pattern** — single DB transaction, then relay to Kafka (no dual-write)
- **Stream processing** — aggregation joins, anomaly detection, optimization recommendations
- **Real-time UI** — Blazor + Radzen over REST and WebSocket
- **Observability** — Grafana, Loki, Promtail, Prometheus (local Compose and Azure AKS)

## How it was built

Specification-driven with [spec-kit](https://github.com/github/spec-kit) and AI (Cursor), on an
abbreviated path — no `plan.md` or `tasks.md`; continuity in [HANDOFF.md](HANDOFF.md#development-process-spec-driven).
Roughly **28 hours** end to end, including implementation, Azure deployment, and documentation.
Design artifacts: [spec.md](specs/001-aetherstream/spec.md),
[architecture.md](specs/001-aetherstream/architecture.md).

## Architecture

### End-to-end data flow

Simulated feeds POST to the write side; the outbox relay publishes to Kafka; Flink jobs
process streams; the API gateway serves queries and pushes live updates to the UI.

```mermaid
flowchart TB
  DS[datasource<br/>turbine 5s · grid 15s]

  subgraph Write["① Write path"]
    direction LR
    WS[write-side<br/>POST /api/ingest/turbine<br/>POST /api/ingest/grid]
    DB[(PostgreSQL<br/>write model + outbox)]
    RL[outbox-relay]
    WS --> DB --> RL
  end

  DS -->|HTTP POST| WS
  RL --> T1{{turbine-events · grid-events}}

  subgraph Flink["② stream-processor"]
    direction LR
    SP_AGG[aggregation join<br/>totalWindPower + gridDemand<br/>→ efficiencyScore]
    SP_ANOM[anomaly detection<br/>vibration spike<br/>grid overload risk]
  end

  T1 --> SP_AGG
  T1 --> SP_ANOM
  SP_AGG --> T2{{energy-state-events}}
  SP_ANOM --> TA{{alerts}}

  subgraph Decide["③ decision-engine"]
    DE[optimization rules<br/>efficiency below target<br/>→ turbine · grid suggestions]
  end

  T2 --> DE
  DE --> T3{{recommendations}}

  subgraph Gateway["④ api-gateway"]
    GW[projections + query APIs<br/>GET /api/energy/latest<br/>GET /api/alerts<br/>GET /api/recommendations<br/>GET /api/turbines/id<br/>WS /ws/realtime]
  end

  T1 --> GW
  T2 --> GW
  TA --> GW
  T3 --> GW

  subgraph UIBlock["Blazor dashboard"]
    UI[Home / — energy overview<br/>/alerts — live alerts<br/>/turbines — turbine grid<br/>/recommendations — suggestions]
  end

  GW --> UI
```

Stages **①–④** run top to bottom. **stream-processor** splits into aggregation (energy state)
and anomaly (alerts) branches; **decision-engine** consumes energy state and emits recommendations.
The gateway projects all topic families, serves the REST paths above, and pushes live events over
WebSocket to the four UI pages.

### Outbox transaction (reliability core)

Kafka is never called inside the business transaction — the relay drains `outbox_events` afterward.

```mermaid
sequenceDiagram
  participant API as Ingest API
  participant DB as PostgreSQL
  participant RL as outbox-relay
  participant K as Kafka

  API->>DB: BEGIN — domain state + outbox row (PENDING)
  API->>DB: COMMIT
  Note over API,K: No Kafka publish in transaction
  RL->>DB: poll PENDING rows
  RL->>K: publish keyed event
  RL->>DB: mark SENT
```

### Deployment (local and Azure)

Same application stack locally (Docker Compose) and on Azure. Blazor and Grafana are
public endpoints; all other services run in the Compose network (local) or inside AKS (Azure).
PostgreSQL is a container locally; on Azure it is **Azure Database for PostgreSQL** outside the cluster.

```mermaid
flowchart TB
  U([Reviewer])

  subgraph Local["Local — Docker Compose"]
    LUI[blazor-dashboard :8086]
    LGF[grafana :3000]
    subgraph LBack["Backbone"]
      LGW[api-gateway]
      LWS[write-side]
      LDS[datasource]
      LRL[outbox-relay]
      LK[kafka]
      LSP[stream-processor]
      LDE[decision-engine]
      LPG[(postgres)]
      LPROM[prometheus]
      LLOKI[loki]
      LPROMT[promtail]
    end
    LUI --> LGW
    LGF --> LPROM
    LGF --> LLOKI
  end

  subgraph Azure["Azure"]
    PG[(PostgreSQL Flexible Server)]
    subgraph AKS["AKS cluster"]
      BZ[blazor-dashboard<br/>public LB]
      GR[grafana<br/>public LB]
      AGW[api-gateway ILB]
      PROM[prometheus]
      LOKI[loki]
      PROMTAIL[promtail]
      WS[write-side]
      DS[datasource]
      RL[outbox-relay]
      KF[kafka]
      SP[stream-processor]
      DE[decision-engine]
      BZ -->|:8085| AGW
      GR --> PROM
      GR --> LOKI
      PROMTAIL --> LOKI
    end
    AKS --> PG
  end

  U -->|docker compose up| Local
  U -->|motivation letter URLs| Azure
```

Full Azure diagram and Terraform runbook: [infra/terraform/README.md](infra/terraform/README.md#architecture).

## Try it locally

**Prerequisite:** Docker Desktop (or Docker Engine + Compose).

```powershell
docker compose -f infra/docker-compose.yml up -d --build
```

First run builds images (several minutes); later runs use the cache. Health check:
`docker compose -f infra/docker-compose.yml ps -a`

### What to open

| What | URL |
|------|-----|
| **Blazor dashboard** (start here) | http://localhost:8086 |
| Grafana (logs + metrics) | http://localhost:3000 (`admin` / `admin`) |
| Kafka UI | http://localhost:8089 |
| API — latest energy state | http://localhost:8085/api/energy/latest |
| API — alerts | http://localhost:8085/api/alerts |
| API — recommendations | http://localhost:8085/api/recommendations |

Live updates use WebSocket `ws://localhost:8085/ws/realtime` (the dashboard connects automatically).

The **datasource** simulator feeds the pipeline continuously — within a minute you should see
energy cards, alerts, and recommendations update without refreshing the Blazor UI.

### Observability quick links

Pre-built dashboard: **Dashboards → AetherStream → AetherStream Logs**.

| What you'll see | Link |
|-----------------|------|
| Datasource simulator (logs every 5–15s) | [Grafana → Loki](http://localhost:3000/explore?orgId=1&schemaVersion=1&panes=%7B%22ds%22%3A%7B%22datasource%22%3A%22loki%22%2C%22range%22%3A%7B%22to%22%3A%22now%22%2C%22from%22%3A%22now-15m%22%7D%2C%22queries%22%3A%5B%7B%22datasource%22%3A%7B%22uid%22%3A%22loki%22%2C%22type%22%3A%22loki%22%7D%2C%22expr%22%3A%22%7Bcontainer%3D%5C%22aether-datasource%5C%22%7D%22%2C%22refId%22%3A%22A%22%7D%5D%7D%7D) |
| Write-side ingest + `correlationId` | [Grafana → Loki](http://localhost:3000/explore?orgId=1&schemaVersion=1&panes=%7B%22ws%22%3A%7B%22datasource%22%3A%22loki%22%2C%22range%22%3A%7B%22to%22%3A%22now%22%2C%22from%22%3A%22now-15m%22%7D%2C%22queries%22%3A%5B%7B%22datasource%22%3A%7B%22uid%22%3A%22loki%22%2C%22type%22%3A%22loki%22%7D%2C%22expr%22%3A%22%7Bcontainer%3D%5C%22aether-write-side%5C%22%7D%20%7C%20json%20%7C%20correlationId%20!%3D%20%5C%22%5C%22%22%2C%22refId%22%3A%22A%22%7D%5D%7D%7D) |
| HTTP request rate (Spring services) | [Grafana → Prometheus](http://localhost:3000/explore?orgId=1&schemaVersion=1&panes=%7B%22pm%22%3A%7B%22datasource%22%3A%22prometheus%22%2C%22range%22%3A%7B%22to%22%3A%22now%22%2C%22from%22%3A%22now-15m%22%7D%2C%22queries%22%3A%5B%7B%22datasource%22%3A%7B%22uid%22%3A%22prometheus%22%2C%22type%22%3A%22prometheus%22%7D%2C%22expr%22%3A%22rate(http_server_requests_seconds_count%7Bjob%3D%5C%22spring-services%5C%22%7D%5B1m%5D)%22%2C%22refId%22%3A%22A%22%7D%5D%7D%7D) |

Same LogQL/PromQL works on Azure AKS (`container` labels match) — swap `localhost:3000`
for the Grafana IP address from the motivation letter and sign in with the Grafana credentials
listed there.

## Hosted demo (Azure)

The same stack runs on AKS. **Live Blazor and Grafana URLs and credentials are in the
motivation letter** — not published here.

Verification checklist: [infra/terraform/SMOKE-VERIFY.md](infra/terraform/SMOKE-VERIFY.md).

Blazor and Grafana run **inside the AKS cluster** (public LoadBalancers). Backend streaming
uses internal LoadBalancers.

### Omitted for price consideration

- Application Gateway and WAF
- Private endpoints (PostgreSQL, ACR, Key Vault)
- Hub-spoke VNet peering
- Premium ACR / private-link registry
- Multi-node AKS and zone redundancy
- App Service or standalone VMs for UI/Grafana — **Blazor and Grafana run in the AKS cluster** instead

## Further reading

| Document | Audience |
|----------|----------|
| [HANDOFF.md](HANDOFF.md) | Contributors — process, build from source, ops notes |
| [specs/001-aetherstream/spec.md](specs/001-aetherstream/spec.md) | Functional requirements |
| [specs/001-aetherstream/architecture.md](specs/001-aetherstream/architecture.md) | Full technical design |
| [infra/terraform/README.md](infra/terraform/README.md) | Azure deploy runbook |

## License

Portfolio / demonstration use. No production warranty implied.
