# AetherStream - Handoff

Cross-session state for the AetherStream build. Update this at the end of every working
session. It is the first thing to read when resuming in a new chat.

Last updated: 2026-06-06 (App Service B1 UI — VM path removed)

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
- **Azure UI:** **App Service B1** (one shared plan, Blazor + Grafana from ACR). No Linux VM.
- **Local demo**: `docker compose -f infra/docker-compose.yml up -d --build` + optional
  `--profile full` (Blazor) and `--profile observability` (Grafana).

## 3. Roadmap (6 phases)

All six phases **DONE**. Azure demo infra on `feat/azure-demo-infrastructure`.

## 4. Current status

**Branch:** `feat/azure-demo-infrastructure`

### Azure demo (2026-06-06)

- [x] **App Service B1** — shared plan, Blazor + Grafana (`enable_app_service = true`)
- [x] VM path removed (`compute-vm` module deleted)
- [x] `app-cd.yml` — deploy AKS + App Service containers
- [x] AKS smoke green (resource limits, Key Vault secrets, Recreate rollout)
- [x] Docs updated — AGW/WAF still **omitted for cost**
- [ ] `terraform apply` with App Service enabled — **blocked**: B1 quota still 0 in northeurope (2026-06-06)
- [x] GitHub vars: `BLAZOR_APP_NAME=aether-demo-blazor`, `GRAFANA_APP_NAME=aether-demo-grafana`
- [ ] End-to-end smoke per [SMOKE-VERIFY.md](infra/terraform/SMOKE-VERIFY.md) — pending App Service apply

## 5. Environment notes / gotchas

- Shell is **PowerShell** on Windows. Use `;` not `&&`.
- **Blazor UI (Azure):** `terraform output dashboard_url` (App Service HTTPS)
- **Grafana (Azure):** `terraform output ops_url`; password in Key Vault `grafana-admin-password`
- **AKS secrets:** `aether-secrets` created from Key Vault — not in kustomize base
- **Single AKS node:** demo overlay uses low CPU requests + Recreate strategy

## 6. Azure deployment (Terraform + CD)

- **Terraform:** [infra/terraform/](infra/terraform/) — `compute-appservice` for UI, `compute-aks` for backbone
- **Kubernetes:** [infra/k8s/overlays/demo](infra/k8s/overlays/demo)
- **CD:** `infra-cd.yml` + `app-cd.yml`

### Deliberately omitted for cost

| Omitted | Why |
|---------|-----|
| **Application Gateway** | ~$200/mo; App Service HTTPS is the edge |
| **WAF** | Requires AGW WAF_v2 |
| **Private endpoints** | ~$7/mo each |
| **Hub-spoke networking** | Single VNet at demo scale |
| **Premium SKUs** | Private-link designs only |

## 7. Open items

- [ ] **Quota:** Request B1 App Service quota in northeurope (Portal → Quotas → App Service → B1 VMs, min 1). CLI needs MFA: `az quota create --resource-name B1 --scope "/subscriptions/4adcc8f4-3fba-4bcf-948e-69b7a5a39fab/providers/Microsoft.Web/locations/northeurope" --limit-object value=2`
- [ ] `terraform apply` — App Service plan + 2 web apps (plan ready; 5 to add)
- [ ] End-to-end smoke after apply (`dashboard_url`, `ops_url`)

Local compose: `docker compose -f infra/docker-compose.yml --profile full --profile observability up -d --build`

## 8. How to resume (copy into a new chat)

```
Read HANDOFF.md first.

Continue Azure deployment for AetherStream on branch feat/azure-demo-infrastructure.
UI is App Service B1 (NOT Linux VM — compute-vm module was deleted). Subscription was
upgraded for Web/App Service quota.

Already live in northeurope: AKS aether-demo-aks, PostgreSQL, ACR aetherdemoacr, Key Vault
aether4adcdemokv, VNet/private DNS. AKS workloads healthy. enable_app_service = true in
terraform.tfvars; app_service_plan_sku = B1 (one shared plan for Blazor + Grafana).

Your tasks:
1. Run terraform apply in infra/terraform/environments/demo (creates App Service plan + 2 web
   apps; destroys leftover VM PIP/NIC/NSG rules if still in state)
2. Set GitHub Environment demo vars: BLAZOR_APP_NAME=aether-demo-blazor,
   GRAFANA_APP_NAME=aether-demo-grafana
3. Smoke-verify per infra/terraform/SMOKE-VERIFY.md (dashboard_url, ops_url outputs)
4. If apply fails on quota, check az appservice list-locations and retry; do NOT revert to VM
5. Keep docs explicit: Application Gateway, WAF, private endpoints, hub-spoke are OMITTED
   FOR COST (see README and infra/terraform/README.md omission tables)
6. Commit uncommitted Terraform/K8s/docs changes and open PR when green

Full local demo still works:
docker compose -f infra/docker-compose.yml --profile full --profile observability up -d --build
```
