# AetherStream — Handoff

Contributor reference for resuming work. Reviewers should start with [README.md](README.md).

Last updated: 2026-06-07

## Project

Real-time wind-energy monitoring: Kafka backbone, Outbox pattern, CQRS, Flink-style stream
processing on the JVM, .NET 10 Blazor + Radzen UI.

| Artifact | Path |
|----------|------|
| Principles | [.specify/memory/constitution.md](.specify/memory/constitution.md) |
| Functional spec | [specs/001-aetherstream/spec.md](specs/001-aetherstream/spec.md) |
| Architecture | [specs/001-aetherstream/architecture.md](specs/001-aetherstream/architecture.md) |

## Stack

| Layer | Choice |
|-------|--------|
| Backend | Java 21, Spring Boot 3.3, Apache Flink 1.19, Kafka (KRaft), PostgreSQL 16 |
| UI | .NET 10 Blazor Server + Radzen (REST + WebSocket) |
| Build | Maven multi-module reactor + committed `mvnw` |
| Reliability | Outbox pattern, at-least-once delivery, idempotent consumers |
| Local runtime | `docker compose -f infra/docker-compose.yml up -d --build` |
| Cloud runtime | Azure AKS + Terraform; GitHub Actions OIDC deploy |
| Public edge | AKS LoadBalancers for Blazor + Grafana |
| Observability | Grafana + Loki + Promtail + Prometheus (Compose and AKS) |

Repo: https://github.com/asaleh-lab/AetherStream

## Deployment layout

| Path | Purpose |
|------|---------|
| [infra/docker-compose.yml](infra/docker-compose.yml) | Full local stack |
| [infra/k8s/overlays/demo](infra/k8s/overlays/demo) | AKS manifests |
| [infra/terraform/](infra/terraform/) | Azure platform (AKS, PostgreSQL, ACR, Key Vault) |
| [infra/terraform/README.md](infra/terraform/README.md) | Deploy runbook |
| [infra/terraform/SMOKE-VERIFY.md](infra/terraform/SMOKE-VERIFY.md) | Post-deploy verification |

Live Azure URLs and Grafana credentials: **motivation letter** (not in repo).

## Contributor notes

- Shell: **PowerShell** on Windows — chain commands with `;` not `&&`.
- AKS secrets (`aether-secrets`, `grafana-secrets`) are injected from Key Vault in CD; not in kustomize base.
- Demo overlay targets a single AKS node (~1.9 vCPU allocatable). If pods stay `Pending` with `Insufficient cpu`, adjust [resource-limits-patch.yaml](infra/k8s/overlays/demo/resource-limits-patch.yaml) or set `aks_node_count = 2`.
- Git workflow: feature branches → PR → `main`, Conventional Commits.
