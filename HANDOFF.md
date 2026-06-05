# AetherStream - Handoff

Cross-session state for the AetherStream build. Update this at the end of every working
session. It is the first thing to read when resuming in a new chat.

Last updated: 2026-06-05 (end of session 3 — **Phase 2 complete**)

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

## 3. Roadmap (6 phases)

1. **Infra & skeleton** — **DONE** (PR #1).
2. **Write side + Outbox** — **DONE** (this branch). Command handlers, domain persistence, transactional outbox writes.
3. **Outbox relay** — idempotent batched publish, retries, DLQ.
4. **Stream processing (Flink)** — aggregation join, anomaly detection, decision engine.
5. **Query side + real-time gateway** — read-model projections, query APIs, WebSocket push.
6. **Blazor UI live + Testcontainers tests + correlation-id propagation + metrics.

## 4. Current status

**Branch:** `phase-2/write-side-outbox`  
**Base:** `main` (includes Phase 1 via fast-forward merge from `phase-1/infra-skeleton`)

### Phase 2 — complete

- [x] `SpringCommandBus` / `SpringQueryBus` in `core/infrastructure`
- [x] Commands: `RecordTurbineTelemetryCommand`, `RecordWeatherReadingCommand`, `RecordGridLoadCommand`
- [x] Handlers: `@Transactional` write model + `OutboxEvent.pending(...)` via `OutboxWriter` port
- [x] JPA adapters: `JpaOutboxAppender`, `JpaOutboxWriter`, `JpaTurbineStateStore`
- [x] Event envelope serialization (`EventEnvelope` + Jackson) with correlation id
- [x] `POST /api/ingest/weather|turbine|grid` on each ingestion service
- [x] Simulated turbine/grid producers + weather polling skeleton
- [x] Testcontainers integration test: command -> `turbine_state` + `outbox_events` PENDING row
- [x] Correlation ID filter (`X-Correlation-Id` header + MDC)

### Verified (2026-06-05)

```powershell
.\mvnw.cmd package                              # OK (includes WriteSideOutboxIntegrationTest)
docker compose -f infra/docker-compose.yml config # OK
```

### Phase 3 — start here (next chat)

1. Open PR for `phase-2/write-side-outbox` -> `main`.
2. Implement outbox relay: poll `PENDING` with `FOR UPDATE SKIP LOCKED`, batch publish to Kafka.
3. Map `aggregate_type`/`event_type` -> topic; mark `SENT` / `FAILED`; DLQ on exhausted retries.
4. Integration test with Testcontainers (Postgres + Kafka): outbox row -> Kafka topic.

**Do not** implement Flink stream processing in Phase 3 — that is Phase 4.

## 5. Environment notes / gotchas

- Shell is **PowerShell** on Windows. Use `;` not `&&`.
- Maven lives at `C:\Users\asama\tools\apache-maven-3.9.9` (User PATH). Prefer **`.\mvnw.cmd`** in-repo.
- Fresh shells may need PATH refresh:
  ```powershell
  $env:Path = [Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [Environment]::GetEnvironmentVariable("Path","User")
  $env:JAVA_HOME = [Environment]::GetEnvironmentVariable("JAVA_HOME","Machine")
  ```
- Flyway migrations: `core/infrastructure/src/main/resources/db/migration/V1__init.sql`
- Blazor template is **`dotnet new blazor --interactivity Server`** (not legacy `blazorserver`).
- Service ports: gateway 8080, weather 8081, turbine 8082, grid 8083, relay 8084, kafka-ui 8089.
- Kafka host bootstrap: `localhost:9094` (dual listeners — Docker-internal clients use `kafka:9092`).
- Outbox `payload` column is JSONB; entity uses `@JdbcTypeCode(SqlTypes.JSON)`.
- Disable simulators for manual API testing: `aetherstream.simulation.enabled=false` or `aetherstream.weather.polling.enabled=false`.

## 6. Open items / blockers

- PR #1 (Phase 1) may still be open on GitHub; merge when ready — Phase 2 branch already contains Phase 1 code.
- Phase 3 needs `docker compose up` for relay smoke tests against Kafka.

## 7. How to resume (copy into a new chat)

```
Continue AetherStream Phase 3 (outbox relay).
Read HANDOFF.md, specs/001-aetherstream/, and .specify/memory/constitution.md.
Branch from main: phase-3/outbox-relay (after merging Phase 2 PR).
Implement idempotent batched outbox relay with retries and DLQ per architecture doc.
Commit conventionally and push to asaleh-lab/AetherStream.
```

## 8. Commit history (Phase 2, chronological)

```text
feat(core): add ingest commands, handlers, and outbox/turbine ports
feat(infra): implement CQRS buses, JPA outbox adapters, and correlation filter
feat(services): add ingest REST APIs and data producers for weather/turbine/grid
test(infra): add Testcontainers write-side outbox integration test
docs: update HANDOFF for Phase 2 completion
```
