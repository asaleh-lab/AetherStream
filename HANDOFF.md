# AetherStream - Handoff

Cross-session state for the AetherStream build. Update this at the end of every working
session. It is the first thing to read when resuming in a new chat.

Last updated: 2026-06-05 (docker compose full-profile fixes on `fix/docker-compose-full-stack`)

## 1. What this project is

A portfolio-grade, real-time wind-energy monitoring platform demonstrating an event-driven
streaming architecture: Kafka backbone, Outbox pattern, CQRS, Flink-style stream
processing on the JVM, with a .NET Blazor + Radzen real-time UI. Authoritative specs:

- Principles: [.specify/memory/constitution.md](.specify/memory/constitution.md)
- Functional spec: [specs/001-aetherstream/spec.md](specs/001-aetherstream/spec.md)
- Architecture: [specs/001-aetherstream/architecture.md](specs/001-aetherstream/architecture.md)

## 2. Locked decisions

- Stack: **Java 21 + Spring Boot 3.3 + Apache Flink 1.19 + Kafka (KRaft) + PostgreSQL 16**
  backend; **.NET 10 Blazor Server + Radzen** UI over REST + WebSocket. (Hybrid polyglot.)
- Build: **Maven multi-module reactor** + committed Maven Wrapper (`mvnw`).
- Reliability: Outbox pattern, at-least-once + idempotent consumers (not end-to-end EOS).
- Process: **real spec-kit** (`.specify/`), **HANDOFF.md** for continuity.
- Git: **phase-based feature branches -> PR -> main**, Conventional Commits, small/single-concern.
- GitHub: **https://github.com/asaleh-lab/AetherStream** (public).
- **Two-part service layout** (clarity for demos and onboarding):
  1. **`datasource`** — one thin Spring Boot producer (no PostgreSQL, domain, or CQRS). Runs three
     independent schedulers at real-world intervals: weather **GET poll** (~60s), turbine telemetry
     (~5s), grid load (~15s). Each POSTs JSON to write-side.
  2. **Write-side backbone** (`write-side`, then `outbox-relay`, Flink jobs, `api-gateway`) —
     `application.yml`, structured logging, CQRS command bus, domain, JPA, transactional outbox,
     Kafka relay, stream processing, query APIs. All ingest REST endpoints live on **write-side**.
- **Local demo**: `docker compose -f infra/docker-compose.yml up -d --build` starts infra +
  **write-side** + **datasource** + **outbox-relay** + **stream-processor** + **api-gateway**.
  Add `--profile full` for the **Blazor dashboard** (port 8086). Reviewers need Docker only.

## 3. Roadmap (6 phases)

1. **Infra & skeleton** — **DONE** (PR #1).
2. **Write side + Outbox** — **DONE** (PR #2).
3. **Outbox relay** — **DONE** (PR #3).
4. **Stream processing (Flink)** — **DONE** (PR #4). Aggregation join + anomaly detection;
   `decision-engine` still skeleton (defer or fold into later phase).
5. **Query side + real-time gateway** — **DONE** (PR #5). Read-model Kafka consumers,
   query REST APIs, WebSocket push, compose service.
6. **Blazor UI live + Testcontainers tests + correlation-id propagation + metrics** — **DONE** (PR #6).

## 4. Current status

**Branch:** `fix/docker-compose-full-stack` (docker compose deploy fixes; PR pending)  
**Base:** `main` (Phase 6 merged — all 6 phases complete)  
**Optional follow-up:** `decision-engine` optimization recommendations (P3 in spec).

### Docker compose fixes (2026-06-05)

- [x] **Blazor image build** — `.dockerignore` no longer excludes all of `ui/`; only
  `ui/blazor-dashboard/bin` and `obj` are ignored so `Dockerfile.blazor-dashboard` can
  `COPY` project sources.
- [x] **stream-processor crash loop** — explicit `flink-connector-base` dependency (compile
  scope) in shaded fat jar; `flink-connector-kafka` marks it `provided` for cluster deploys
  but this job runs standalone via `java -jar`.
- [x] **Spring Boot repackage** — `api-gateway` and `outbox-relay` POMs aligned with
  `write-side` / `datasource` so Docker Maven builds produce executable jars reliably.

### Phase 6 — complete (merged PR #6)

- [x] Blazor Server + Radzen dashboard wired to gateway REST + WebSocket
- [x] Live energy cards, alerts panel, turbine DataGrid, regional weather/energy view
- [x] `blazor-dashboard` in [infra/docker-compose.yml](infra/docker-compose.yml) (`--profile full`, port 8086)
- [x] Correlation-id propagation tests: outbox relay Kafka header + api-gateway consumer header
- [x] Blazor `/health` endpoint; gateway connection status badges in UI

### Verified (2026-06-05)

```powershell
$env:JAVA_HOME = "C:\Program Files\Amazon Corretto\jdk21.0.11_10"
.\mvnw.cmd -pl services/api-gateway,services/outbox-relay -am test   # OK
dotnet build ui/blazor-dashboard/AetherStream.Dashboard.csproj         # OK
docker compose -f infra/docker-compose.yml config                      # OK
docker compose -f infra/docker-compose.yml --profile full up -d --build  # OK (all healthy)
docker compose -f infra/docker-compose.yml --profile full ps -a        # stream-processor Up
```

### Phase 3 — complete (merged PR #3)

- [x] Outbox relay: poll `PENDING` with `FOR UPDATE SKIP LOCKED`, batch publish to Kafka
- [x] Retries across poll cycles; exhausted failures → `dead-letter-events` + `FAILED`
- [x] `outbox-relay` in [infra/docker-compose.yml](infra/docker-compose.yml) (port 8084)
- [x] Testcontainers integration test (Postgres + Kafka): outbox row → Kafka topic

### Phase 4 — complete (merged PR #4)

- [x] Flink `stream-processor`: consume `turbine-events`, `weather-events`, `grid-events`
- [x] Keyed-by-region aggregation (Flink state): `totalWindPower`, `gridDemand`, `efficiencyScore`
  → `energy-state-events`
- [x] Anomaly rules: vibration spike, turbine failure pattern, grid overload → `alerts`
- [x] Event-time watermarks with 5s allowed lateness
- [x] `stream-processor` in [infra/docker-compose.yml](infra/docker-compose.yml)
- [x] Pipeline + operator harness tests (aggregation, anomaly, envelope parsing)
- [ ] Decision engine (`decision-engine` skeleton → optimization recommendations; deferred)

### Phase 5 — complete (merged PR #5)

- [x] Kafka consumers: `energy-state-events` → `energy_state_snapshot`, `alerts` → `alerts`
  (idempotent by event id)
- [x] Query REST APIs on `api-gateway`: `GET /api/energy/latest`, `/api/alerts`, `/api/turbines/{id}`
- [x] WebSocket push at `/ws/realtime` (energy-state + alert messages)
- [x] CQRS query handlers via `QueryBus` + read-model ports
- [x] `api-gateway` in [infra/docker-compose.yml](infra/docker-compose.yml) (port 8085)
- [x] Testcontainers integration test: Kafka event → read-model projection

## 5. Environment notes / gotchas

- Shell is **PowerShell** on Windows. Use `;` not `&&`.
- Prefer **`.\mvnw.cmd`** for local Java dev; **`docker compose`** for reviewer/demo path.
- Set **`JAVA_HOME`** to JDK 21 if `mvnw` fails (e.g. Amazon Corretto 21).
- Flyway migrations: `core/infrastructure/src/main/resources/db/migration/V1__init.sql`
- **Write-side ingest** (CQRS + outbox): `http://localhost:8080/api/ingest/{weather|turbine|grid}`
- **Query APIs** (read side): `http://localhost:8085/api/energy/latest`, `/api/alerts`,
  `/api/turbines/{id}`
- **WebSocket** (real-time push): `ws://localhost:8085/ws/realtime`
- **Blazor UI**: `http://localhost:8086` (compose `--profile full`) or `dotnet run --project ui/blazor-dashboard`
- **Compose services**: `write-side` (8080), `datasource` (8081), `outbox-relay` (8084),
  `api-gateway` (8085), `blazor-dashboard` (8086, profile `full`), `stream-processor` (Flink job, no HTTP port).
- Datasource env: `AETHER_WRITE_SIDE_URL=http://write-side:8080` (Docker internal).
- Datasource intervals (defaults): weather poll 60s, turbine 5s, grid 15s — see
  `services/datasource/src/main/resources/application.yml`.
- Kafka: host `localhost:9094`, Docker-internal `kafka:9092`.
- Stream processor env: `AETHER_KAFKA_BOOTSTRAP`, `AETHER_VIBRATION_THRESHOLD` (default 1.0),
  `AETHER_KAFKA_OFFSET_RESET` (`latest` in compose, `earliest` for replay).
- Turbine→region mapping (stream join): T-001/T-002 → `north-sea`, T-003 → `baltic`.
- First `docker compose up --build` is slow (Maven + .NET inside images); subsequent runs use cache.
- Stop host Java processes before compose if ports 8080–8081 or 8085–8086 are already taken.
- **`.dockerignore`**: must not blanket-ignore `ui/` — Blazor dashboard Docker build needs
  `ui/blazor-dashboard/` in the build context.
- **Flink shaded jars** (`stream-processor`, `decision-engine`): declare `flink-connector-base`
  explicitly; the Kafka connector's `provided` scope omits it from the fat jar.
- **`kafka-init`** exits 0 after topic creation — expected; not a failure.
- If a container "flashes" in Docker Desktop, check `docker logs <name>` — usually a crash
  loop from a missing classpath dependency or failed health check.

## 6. Open items / blockers

- All 6 phases merged to `main`.
- `fix/docker-compose-full-stack` — open PR and merge docker compose deploy fixes.
- `decision-engine` still skeleton (optimization recommendations — optional follow-up).

## 7. How to resume (copy into a new chat)

```
AetherStream Phase 6 is complete. Merge fix/docker-compose-full-stack if not on main yet.
Read HANDOFF.md for optional follow-ups (decision-engine).
```

## 8. Recent commits (chronological)

```text
feat(core): add ingest commands, handlers, and outbox/turbine ports
feat(infra): implement CQRS buses, JPA outbox adapters, and correlation filter
feat(services): add ingest REST APIs and data producers for weather/turbine/grid
test(infra): add Testcontainers write-side outbox integration test
docs: update HANDOFF for Phase 2 completion
infra(docker): containerize weather/turbine/grid data sources in compose
refactor(services): split thin datasource producers from write-side backbone
refactor(services): merge datasource feeds into single service with distinct intervals
fix(docker): copy core modules for datasource Maven reactor
docs: sync architecture and handoff for Phase 2 cleanup audit
feat(infra): add outbox polling, Kafka publisher, and topic router
feat(services): implement outbox relay with retries and DLQ routing
test(services): add Testcontainers outbox relay integration test
infra(docker): add outbox-relay service to compose
docs: update HANDOFF for Phase 3 outbox relay
feat(phase-3): outbox relay to Kafka with retries and DLQ  [PR #3 merged]
feat(stream-processor): Flink aggregation join and anomaly detection  [PR #4 merged]
feat(api-gateway): read-model projections, query APIs, and WebSocket push  [PR #5 merged]
feat(blazor-dashboard): live UI wired to gateway REST and WebSocket  [PR #6 merged]
fix(docker): restore full compose profile builds and stream-processor startup  [branch fix/docker-compose-full-stack]
```
