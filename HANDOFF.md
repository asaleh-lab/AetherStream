# AetherStream - Handoff

Cross-session state for the AetherStream build. Update this at the end of every working
session. It is the first thing to read when resuming in a new chat.

Last updated: 2026-06-05 (Phase 2 cleanup audit — single datasource, docs synced)

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
  **write-side** + **datasource**. Reviewers need Docker only.

## 3. Roadmap (6 phases)

1. **Infra & skeleton** — **DONE** (PR #1).
2. **Write side + Outbox** — **DONE**. Central `write-side` ingest APIs, command handlers,
   domain persistence, transactional outbox writes; single `datasource` producer.
3. **Outbox relay** — idempotent batched publish, retries, DLQ.
4. **Stream processing (Flink)** — aggregation join, anomaly detection, decision engine.
5. **Query side + real-time gateway** — read-model projections, query APIs, WebSocket push.
6. **Blazor UI live + Testcontainers tests + correlation-id propagation + metrics.

## 4. Current status

**Branch:** `phase-2/write-side-outbox` (or successor)  
**Base:** `main`

### Phase 2 — complete

- [x] CQRS command bus, ingest handlers, outbox writes (on **write-side**)
- [x] Testcontainers write-side integration test
- [x] Correlation ID filter
- [x] **Single `datasource` service** — weather poll + turbine + grid simulators POST to write-side
- [x] **write-side** in [infra/docker-compose.yml](infra/docker-compose.yml) (port 8080)
- [x] [scripts/smoke-ingest.ps1](scripts/smoke-ingest.ps1) — compose up + POST to write-side

### Verified (2026-06-05)

```powershell
.\mvnw.cmd package                              # OK (includes WriteSideOutboxIntegrationTest)
docker compose -f infra/docker-compose.yml config # OK
.\scripts\smoke-ingest.ps1 -SkipCommit          # compose + ingest smoke (after image build)
```

### Phase 3 — start here (next chat)

1. Open/merge Phase 2 PR if not done.
2. Implement outbox relay: poll `PENDING` with `FOR UPDATE SKIP LOCKED`, batch publish to Kafka.
3. Add `outbox-relay` to docker-compose when relay is ready.
4. Integration test with Testcontainers (Postgres + Kafka): outbox row -> Kafka topic.

**Do not** implement Flink stream processing in Phase 3 — that is Phase 4.

### Later compose work (natural follow-on)

- `outbox-relay`, `stream-processor`, `decision-engine`, `api-gateway`, Blazor UI — add to
  compose as each phase lands (goal: full stack in one compose).
- Optional compose **profile** `full` when all services are containerized (SC-004).

## 5. Environment notes / gotchas

- Shell is **PowerShell** on Windows. Use `;` not `&&`.
- Prefer **`.\mvnw.cmd`** for local Java dev; **`docker compose`** for reviewer/demo path.
- Flyway migrations: `core/infrastructure/src/main/resources/db/migration/V1__init.sql`
- **Write-side ingest** (CQRS + outbox): `http://localhost:8080/api/ingest/{weather|turbine|grid}`
- **Compose services**: `write-side` (8080), `datasource` (8081).
- Datasource env: `AETHER_WRITE_SIDE_URL=http://write-side:8080` (Docker internal).
- Datasource intervals (defaults): weather poll 60s, turbine 5s, grid 15s — see
  `services/datasource/src/main/resources/application.yml`.
- Kafka: host `localhost:9094`, Docker-internal `kafka:9092`.
- First `docker compose up --build` is slow (Maven inside images); subsequent runs use cache.
- Stop host Java processes before compose if ports 8080–8081 are already taken.

## 6. Open items / blockers

- PR #1 (Phase 1) may still be open on GitHub; merge when ready.
- Outbox relay not yet in compose — Phase 3 deliverable.

## 7. How to resume (copy into a new chat)

```
Continue AetherStream Phase 3 (outbox relay).
Read HANDOFF.md, specs/001-aetherstream/, and .specify/memory/constitution.md.
Implement idempotent batched outbox relay with retries and DLQ per architecture doc.
Add relay to docker-compose when ready. Commit conventionally and push to asaleh-lab/AetherStream.
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
```
