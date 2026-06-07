# AetherStream - Handoff

Cross-session state for the AetherStream build. Update this at the end of every working
session. It is the first thing to read when resuming in a new chat.

Last updated: 2026-06-07 (AKS Loki + Promtail — Grafana log parity with local compose)

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
- **Azure UI:** **AKS public LoadBalancers** for Blazor + Grafana (no App Service — B1 quota blocked).
- **Local demo**: `docker compose -f infra/docker-compose.yml up -d --build` + optional
  `--profile full` (Blazor) and `--profile observability` (Grafana).

## 3. Roadmap (6 phases)

All six phases **DONE**. Azure demo infra on `feat/azure-demo-infrastructure`.

## 4. Current status

**Branch:** `feat/azure-demo-infrastructure`

### Azure demo (2026-06-06)

- [x] **UI on AKS** — Blazor + Grafana as Deployments with public LoadBalancer Services
- [x] **Observability on AKS** — Loki + Promtail + Prometheus in-cluster; Grafana provisions Loki + Prometheus (parity with local `--profile observability`)
- [x] App Service path removed (`compute-appservice` module deleted; B1 quota unavailable in northeurope)
- [x] `app-cd.yml` — single AKS deploy job (backbone + UI)
- [x] AKS backbone smoke green
- [x] Docs updated — AGW/WAF still **omitted for cost**
- [ ] `terraform apply` — tear down unused App Service identities / appsvc subnet
- [ ] End-to-end smoke per [SMOKE-VERIFY.md](infra/terraform/SMOKE-VERIFY.md)

## 5. Environment notes / gotchas

- Shell is **PowerShell** on Windows. Use `;` not `&&`.
- **Blazor + Grafana (Azure):** public LoadBalancer URLs and login credentials are in the **motivation letter** (not in repo docs)
- **AKS secrets:** `aether-secrets` + `grafana-secrets` (from Key Vault in CD) — not in kustomize base
- **Single AKS node:** demo overlay uses low CPU requests + Recreate strategy; Loki/Promtail/Grafana/Prometheus share the node (~1.9 vCPU allocatable) — if pods stay `Pending` with `Insufficient cpu`, trim requests in `resource-limits-patch.yaml` or set `aks_node_count = 2`

## 6. Azure deployment (Terraform + CD)

- **Terraform:** [infra/terraform/](infra/terraform/) — AKS + platform only (no App Service)
- **Kubernetes:** [infra/k8s/overlays/demo](infra/k8s/overlays/demo) — backbone + UI
- **CD:** `infra-cd.yml` + `app-cd.yml`

### Deliberately omitted for cost

| Omitted | Why |
|---------|-----|
| **Application Gateway** | ~$200/mo; AKS LoadBalancer HTTP is the edge |
| **WAF** | Requires AGW WAF_v2 |
| **Private endpoints** | ~$7/mo each |
| **Hub-spoke networking** | Single VNet at demo scale |
| **App Service** | B1 quota 0 in northeurope on this subscription |
| **Premium SKUs** | Private-link designs only |

## 7. Open items

- [ ] `terraform apply` — remove orphaned App Service identities / appsvc subnet from state
- [ ] Deploy UI manifests + smoke verify (URLs in motivation letter)
- [ ] Merge PR #11 (update description for AKS UI)

Local compose: `docker compose -f infra/docker-compose.yml --profile full --profile observability up -d --build`
