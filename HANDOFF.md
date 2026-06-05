# AetherStream - Handoff

Cross-session state for the AetherStream build. Update this at the end of every working
session. It is the first thing to read when resuming in a new chat.

Last updated: 2026-06-05 (Phase 4 stream processor — Flink aggregation + anomaly)

## 1. What this project is

A portfolio-grade, real-time wind-energy monitoring platform demonstrating an event-driven
streaming architecture: Kafka backbone, Outbox pattern, CQRS, Flink-style stream
processing on the JVM, with a .NET Blazor + Radzen real-time UI. Authoritative specs:

- Principles: [.specify/memory/constitution.md](.specify/memory/constitution.md)
- Functional spec: [specs/001-aetherstream/spec.md](specs/001-aetherstream/spec.md)
- Architecture: [specs/001-aetherstream/architecture.md](specs/001-aetherstream/architecture.md)

## 2. Locked decisions

- Stack: **Java 21 + Spring Boot 3.3 + Apache Flink 1.19 + Kafka (KRaft) + PostgreSQL 16**
  backend; **.NET 8 Blazor Server + Radzen** UI over REST + WebSocket. (Hybrid polyglot.)
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
  **write-side** + **datasource** + **outbox-relay** + **stream-processor**. Reviewers need Docker only.

## 3. Roadmap (6 phases)

1. **Infra & skeleton** — **DONE** (PR #1).
2. **Write side + Outbox** — **DONE** (PR #2).
3. **Outbox relay** — **DONE** (PR #3).
4. **Stream processing (Flink)** — **IN PROGRESS** on `phase-4/stream-processor`. Aggregation join
   + anomaly detection implemented; decision engine still skeleton.
5. **Query side + real-time gateway** — read-model projections, query APIs, WebSocket push.
6. **Blazor UI live + Testcontainers tests + correlation-id propagation + metrics.

## 4. Current status

**Branch:** `phase-4/stream-processor`  
**Base:** `main`

### Phase 3 — complete (merged PR #3)

- [x] Outbox relay: poll `PENDING` with `FOR UPDATE SKIP LOCKED`, batch publish to Kafka
- [x] Retries across poll cycles; exhausted failures → `dead-letter-events` + `FAILED`
- [x] `outbox-relay` in [infra/docker-compose.yml](infra/docker-compose.yml) (port 8084)
- [x] Testcontainers integration test (Postgres + Kafka): outbox row → Kafka topic

### Phase 4 — in progress

- [x] Flink `stream-processor`: consume `turbine-events`, `weather-events`, `grid-events`
- [x] Keyed-by-region aggregation (Flink state): `totalWindPower`, `gridDemand`, `efficiencyScore`
  → `energy-state-events`
- [x] Anomaly rules: vibration spike, turbine failure pattern, grid overload → `alerts`
- [x] Event-time watermarks with 5s allowed lateness
- [x] `stream-processor` in [infra/docker-compose.yml](infra/docker-compose.yml)
- [x] Pipeline + operator harness tests (aggregation, anomaly, envelope parsing)
- [ ] Decision engine (`decision-engine` skeleton → optimization recommendations)
- [ ] Open PR and merge Phase 4 (do this at end of session)

### Verified (2026-06-05)

```powershell
.\mvnw.cmd -pl services/stream-processor -am test   # OK (pipeline + harness tests)
docker compose -f infra/docker-compose.yml config    # OK
docker build -f infra/docker/Dockerfile.stream-processor -t aetherstream/stream-processor:local .  # OK
```

### Phase 5 — start here (after Phase 4 PR)

1. Implement `api-gateway` read-model consumers (energy-state, alerts projections).
2. Add query REST APIs and WebSocket push.
3. Add `api-gateway` to docker-compose.

**Do not** implement Blazor UI in Phase 5 — that is Phase 6.

### Later compose work (natural follow-on)

- `decision-engine`, `api-gateway`, Blazor UI — add to compose as each phase lands.
- Optional compose **profile** `full` when all services are containerized (SC-004).

## 5. Environment notes / gotchas

- Shell is **PowerShell** on Windows. Use `;` not `&&`.
- Prefer **`.\mvnw.cmd`** for local Java dev; **`docker compose`** for reviewer/demo path.
- Flyway migrations: `core/infrastructure/src/main/resources/db/migration/V1__init.sql`
- **Write-side ingest** (CQRS + outbox): `http://localhost:8080/api/ingest/{weather|turbine|grid}`
- **Compose services**: `write-side` (8080), `datasource` (8081), `outbox-relay` (8084),
  `stream-processor` (Flink job, no HTTP port).
- Datasource env: `AETHER_WRITE_SIDE_URL=http://write-side:8080` (Docker internal).
- Datasource intervals (defaults): weather poll 60s, turbine 5s, grid 15s — see
  `services/datasource/src/main/resources/application.yml`.
- Kafka: host `localhost:9094`, Docker-internal `kafka:9092`.
- Stream processor env: `AETHER_KAFKA_BOOTSTRAP`, `AETHER_VIBRATION_THRESHOLD` (default 1.0),
  `AETHER_KAFKA_OFFSET_RESET` (`latest` in compose, `earliest` for replay).
- Turbine→region mapping (stream join): T-001/T-002 → `north-sea`, T-003 → `baltic`.
- First `docker compose up --build` is slow (Maven inside images); subsequent runs use cache.
- Stop host Java processes before compose if ports 8080–8081 are already taken.

## 6. Open items / blockers

- Phase 3 PR #3 merged to `main`.
- Phase 4 PR not yet opened.
- `decision-engine` still skeleton (optimization recommendations — finish in Phase 4 or defer).

## 7. How to resume (copy into a new chat)

```
Continue AetherStream Phase 4 (Flink stream processing).
Read HANDOFF.md, specs/001-aetherstream/, and .specify/memory/constitution.md.
At the end of the session: commit, open the Phase 4 PR, and merge it right away.
If Phase 4 PR is already merged, start Phase 5 (query side + api-gateway).
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
```
