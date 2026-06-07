# AetherStream — Handoff

Cross-session continuity for a **specification-driven** build. **Reviewers start with
[README.md](README.md)** for how to run the demo; use this file for process context and
contributor notes.

Last updated: 2026-06-07

## Project

Real-time wind-energy monitoring: Kafka backbone, Outbox pattern, CQRS, Flink-style stream
processing on the JVM, .NET 10 Blazor + Radzen UI.

## Development process (spec-driven)

[spec-kit](https://github.com/github/spec-kit) is initialized (`.specify/`, Cursor skills).
The build stayed specification-driven, but **abbreviated** the canonical
`plan.md` → `tasks.md` pipeline to move fast with AI (Cursor):

| Step | Artifact | Path |
|------|----------|------|
| Principles | Constitution | [.specify/memory/constitution.md](.specify/memory/constitution.md) |
| Specify | Functional spec | [specs/001-aetherstream/spec.md](specs/001-aetherstream/spec.md) |
| Plan (merged) | Architecture | [specs/001-aetherstream/architecture.md](specs/001-aetherstream/architecture.md) |
| Continuity | **This handoff** | `HANDOFF.md` — locked decisions, deployment layout, gotchas across sessions |
| Implement | Codebase | Services, UI, `infra/` |

**Not in repo:** `plan.md`, `tasks.md` (no generated task checklist). `architecture.md`
covers what a full spec-kit plan would hold; **HANDOFF.md** carries session-to-session
state that would otherwise live in tasks and handoff updates during a longer spec-kit run.

That is intentional for a short AI-assisted timeline: specs constrain design; the running
system is the proof of implementation.

## Implemented stack

| Layer | Components |
|-------|------------|
| Producers | `datasource` (turbine 5s, grid 15s simulators) |
| Write path | `write-side`, `outbox-relay`, PostgreSQL, Flyway |
| Streaming | Kafka (KRaft), `stream-processor`, `decision-engine` |
| Read path | `api-gateway` (REST + WebSocket), Blazor dashboard |
| Observability | Grafana, Loki, Promtail, Prometheus |
| Local | `docker compose -f infra/docker-compose.yml up -d --build` |
| Azure | AKS + Terraform; manifests `infra/k8s/overlays/demo` |

**APIs (implemented):** `POST /api/ingest/turbine|grid` (write-side);
`GET /api/energy/latest`, `/api/alerts`, `/api/recommendations`, `/api/turbines/{id}` (gateway);
`ws://…/ws/realtime` (gateway → UI).

Repo: https://github.com/asaleh-lab/AetherStream

## Deployment layout

| Path | Purpose |
|------|---------|
| [infra/docker-compose.yml](infra/docker-compose.yml) | Full local stack (15 containers) |
| [infra/k8s/base/](infra/k8s/base/) | AKS manifests (backbone + UI + observability) |
| [infra/k8s/overlays/demo](infra/k8s/overlays/demo) | Demo image tags + resource limits |
| [infra/terraform/](infra/terraform/) | Azure platform (AKS, PostgreSQL, ACR, Key Vault) |
| [infra/terraform/README.md](infra/terraform/README.md) | Deploy runbook + architecture diagram |
| [infra/terraform/SMOKE-VERIFY.md](infra/terraform/SMOKE-VERIFY.md) | Post-deploy verification |

Live Azure URLs and Grafana credentials: **motivation letter** (not in repo).

**Omitted for price consideration** (same as [README.md](README.md)): Application Gateway/WAF,
private endpoints, hub-spoke networking, Premium ACR, multi-node/zone-redundant AKS; Blazor and
Grafana run in-cluster on AKS instead of App Service or standalone VMs.

## Build from source (contributors)

- **Java:** JDK 21 — `.\mvnw.cmd -DskipTests package`
- **UI:** .NET 10 SDK — `dotnet build ui/blazor-dashboard`
- **UI dev server:** `dotnet run --project ui/blazor-dashboard` (default port 5000)
- **Smoke script:** `.\scripts\smoke-ingest.ps1 -SkipCommit`

## Contributor notes

- Shell: **PowerShell** on Windows — chain commands with `;` not `&&`.
- AKS secrets (`aether-secrets`, `grafana-secrets`) are injected from Key Vault in CD; not in kustomize base.
- Demo overlay targets a single AKS node (~1.9 vCPU allocatable). If pods stay `Pending` with `Insufficient cpu`, adjust [resource-limits-patch.yaml](infra/k8s/overlays/demo/resource-limits-patch.yaml) or set `aks_node_count = 2`.
- Prometheus uses job `spring-services` in both Compose and AKS (same PromQL as README Explore links).
- Git workflow: feature branches → PR → `main`, Conventional Commits.
