# AetherStream - Handoff

Cross-session state for the AetherStream build. Update this at the end of every working
session. It is the first thing to read when resuming in a new chat.

Last updated: 2026-06-05 (end of session 1)

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
- Build: **Maven multi-module reactor** + committed Maven Wrapper.
- Reliability: Outbox pattern, at-least-once + idempotent consumers (not end-to-end EOS).
- Process: **real spec-kit** (`.specify/`), **HANDOFF.md** for continuity.
- Git: **phase-based feature branches -> PR -> main**, Conventional Commits, small/single-concern.
- GitHub: public repo target **`asaleh-lab/AetherStream`** (push deferred, see Open items).

## 3. Roadmap (6 phases)

1. **Infra & skeleton** (IN PROGRESS): monorepo, build files, domain model, topics + schema, docker-compose, spec-kit + git + handoff.
2. Write side + Outbox: command handlers, domain persistence, transactional outbox writes.
3. Outbox relay: idempotent batched publish, retries, DLQ.
4. Stream processing (Flink): aggregation join, anomaly detection, decision engine.
5. Query side + real-time gateway: read-model projections, query APIs, WebSocket push.
6. Blazor UI live + Testcontainers tests + correlation-id propagation + metrics.

## 4. Current status (Phase 1)

Branch: `phase-1/infra-skeleton`.

Done:
- [x] Toolchain installed: Corretto JDK 21.0.11, Maven 3.9.9, uv 0.11.19 (+ pre-existing .NET 8, Docker, git, gh).
- [x] git init, `.gitignore`, `.gitattributes`, branch `main` -> `phase-1/infra-skeleton`.
- [x] spec-kit initialized (`.specify/`, cursor-agent skills).
- [x] Constitution authored.
- [x] SPEC + ARCHITECTURE authored.
- [x] HANDOFF.md created (this file).
- [x] GitHub repo created + pushed; draft PR #1 opened.

Not started (next session, in order):
- [ ] Parent Maven reactor `pom.xml` (Java 21, Spring Boot + Flink BOMs) + Maven Wrapper + module dirs.
- [ ] `core/domain` model, `core/application` CQRS bus interfaces, `core/infrastructure` stubs.
- [ ] Spring Boot services scaffold: ingestion-weather/turbine/grid, api-gateway, outbox-relay.
- [ ] Flink modules scaffold: stream-processor, decision-engine.
- [ ] Flyway `V1__init.sql`: outbox_events, turbine_state, energy_state_snapshot, alerts.
- [ ] Kafka topics: `create-topics.sh` + KafkaAdmin `NewTopic` beans (7 topics).
- [ ] `infra/docker-compose.yml`: Kafka KRaft, kafka-ui, postgres:16, topic-init.
- [ ] Blazor Server + Radzen dashboard shell (Dashboard/Turbines/Alerts/Weather placeholders).
- [ ] README (portfolio tone) + final HANDOFF update.
- [ ] Verify builds (`mvn package`, `docker compose config`, `dotnet build`); open PR; push when auth available.

## 5. Environment notes / gotchas

- Shell is **PowerShell** on Windows. Use `;` not `&&`.
- Maven is NOT in the winget source; installed to `C:\Users\asama\tools\apache-maven-3.9.9`
  and added to **User PATH**. JDK 21 set `JAVA_HOME` machine-wide.
- A freshly spawned shell may not see the updated PATH/JAVA_HOME until refreshed. If `mvn`,
  `java`, or `uv` are "not recognized", run:
  ```powershell
  $env:Path = [Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [Environment]::GetEnvironmentVariable("Path","User")
  $env:JAVA_HOME = [Environment]::GetEnvironmentVariable("JAVA_HOME","Machine")
  ```
- spec-kit runs via `uvx --from git+https://github.com/github/spec-kit.git specify ...`.
- Git identity already configured globally (`asama <asamak@outlook.com>`).

## 6. Open items / blockers

- **GitHub: live.** Repo at https://github.com/asaleh-lab/AetherStream (public). `main` and
  `phase-1/infra-skeleton` pushed; draft PR #1 tracks Phase 1. `gh` authenticated as user
  `asaleh-lab` (scopes: repo, workflow, read:org, gist). Push as we commit:
  `git push`. Mark PR ready + merge at phase end; Phase 2 branches from `main`.

## 7. How to resume

1. Read this file, then the constitution, spec, and architecture.
2. Refresh the shell env (section 5) and confirm `java -version`, `mvn -version`, `uv --version`.
3. Continue with the first unchecked item in section 4, committing per item (Conventional Commits).
4. At session end, update sections 4 and 6 here and commit.
