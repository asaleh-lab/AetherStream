# AetherStream - Handoff

Cross-session state for the AetherStream build. Update this at the end of every working
session. It is the first thing to read when resuming in a new chat.

Last updated: 2026-06-05 (end of session 2 — **Phase 1 complete**)

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
- GitHub: **https://github.com/asaleh-lab/AetherStream** (public). Draft **PR #1** tracks Phase 1.

## 3. Roadmap (6 phases)

1. **Infra & skeleton** — **DONE** (this PR). Monorepo, build files, domain model, schema, topics, docker-compose, UI shell, README.
2. **Write side + Outbox** — command handlers, domain persistence, transactional outbox writes.
3. **Outbox relay** — idempotent batched publish, retries, DLQ.
4. **Stream processing (Flink)** — aggregation join, anomaly detection, decision engine.
5. **Query side + real-time gateway** — read-model projections, query APIs, WebSocket push.
6. **Blazor UI live + Testcontainers tests + correlation-id propagation + metrics.

## 4. Current status

**Branch:** `phase-1/infra-skeleton`  
**PR:** https://github.com/asaleh-lab/AetherStream/pull/1 (draft — mark ready + merge to close Phase 1)

### Phase 1 — complete

- [x] Toolchain: Corretto JDK 21, Maven 3.9.9, uv, .NET 8, Docker, gh
- [x] Git + spec-kit + constitution + SPEC + ARCHITECTURE
- [x] Parent Maven reactor + `mvnw` + 10 modules (3 core + 5 Spring + 2 Flink)
- [x] Domain model, CQRS bus interfaces, JPA entities/repos, Flyway `V1__init.sql`
- [x] Spring Boot scaffolds: ingestion-weather/turbine/grid, outbox-relay, api-gateway
- [x] Flink scaffolds: stream-processor, decision-engine (shade jars build)
- [x] Kafka: `KafkaTopicsConfig` (7 topics) + `infra/kafka/create-topics.sh`
- [x] `infra/docker-compose.yml` (Postgres 16, Kafka KRaft, kafka-ui, topic-init)
- [x] Blazor dashboard shell + Radzen (Dashboard, Turbines, Alerts, Weather placeholders)
- [x] README + verification green (see below)

### Verified (2026-06-05)

```powershell
.\mvnw.cmd -DskipTests package          # OK
docker compose -f infra/docker-compose.yml config   # OK
dotnet build ui/blazor-dashboard        # OK
```

### Phase 2 — start here (next chat)

1. Merge PR #1 (or continue on `phase-2/write-side-outbox` branched from `main`).
2. Implement `CommandBus`/`QueryBus` concrete dispatchers in `core/infrastructure`.
3. Add ingestion command handlers: persist write model + `OutboxEvent.pending(...)` in one `@Transactional`.
4. Wire `POST /api/ingest/weather|turbine|grid` on each ingestion service.
5. Add simulated turbine/grid producers and weather API polling skeleton.
6. Integration test with Testcontainers (Postgres): command -> outbox row, no Kafka yet.

**Do not** implement relay or Flink logic in Phase 2 — that is Phases 3–4.

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

## 6. Open items / blockers

- None for Phase 1. PR #1 ready to mark **Ready for review** and merge when you are satisfied.
- Phase 2 needs `docker compose up` for manual smoke tests once handlers exist.

## 7. How to resume (copy into a new chat)

```
Continue AetherStream Phase 2 (write side + outbox).
Read HANDOFF.md, specs/001-aetherstream/, and .specify/memory/constitution.md.
Branch from main: phase-2/write-side-outbox.
Implement command handlers with transactional outbox writes per the architecture doc.
Commit conventionally and push to asaleh-lab/AetherStream.
```

## 8. Commit history (Phase 1, chronological)

```text
chore: initialize repository with .gitignore
docs(spec): bootstrap spec-kit scaffolding for cursor-agent
docs(spec): author AetherStream constitution; add .gitattributes
docs(spec): add functional spec and architecture for AetherStream
docs: add HANDOFF.md cross-session state and roadmap
docs: record GitHub repo + draft PR in handoff
build: add parent Maven reactor, wrapper, and core module skeletons
feat(core): add domain model, CQRS bus + ports, JPA entities and repositories
feat(services): scaffold Spring Boot services (ingestion x3, outbox-relay, api-gateway)
feat(stream): scaffold Flink jobs (stream-processor, decision-engine) with shade packaging
feat(db): add Flyway V1 schema (outbox_events, turbine_state, read models)
feat(infra): add Kafka topic config and docker-compose stack
feat(ui): scaffold Blazor Server dashboard with Radzen placeholders
docs: add README and finalize Phase 1 handoff
```
