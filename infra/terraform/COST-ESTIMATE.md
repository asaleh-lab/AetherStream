# Azure demo cost estimate

**West Europe** monthly costs for the default `terraform.tfvars` (lowest viable demo SKUs).
All figures from **Infracost** only — run locally to refresh.

**Last run:** 2026-06-06 — `infracost breakdown --path infra/terraform/environments/demo`

## Run Infracost

```powershell
infracost auth login   # once
cd infra/terraform/environments/demo
terraform init -backend=false
infracost breakdown --path .
```

## Demo SKUs (terraform.tfvars)

| Variable | Value | Notes |
|---|---|---|
| `app_service_plan_sku` | `B1` | Public Blazor + Grafana (lowest Linux container tier) |
| `aks_vm_size` | `Standard_B2als_v2` | Burstable node |
| `aks_node_count` | `1` | |
| `acr_sku` | `Basic` | Public registry |
| `postgres_sku_name` | `B_Standard_B1ms` | Smallest Flexible Server burstable |
| `log_analytics_retention_days` | `1` | Minimum retention |

## Infracost breakdown

**Total: $67/mo** (31 resources detected, 9 priced, 22 free)

| Resource | Monthly cost |
|---|---:|
| AKS — 1× Standard_B2als_v2 + P10 OS disk | $53 |
| App Service Plan — B1 (Blazor + Grafana) | $13 |
| Private DNS zone (internal) | $0.50 |
| **OVERALL TOTAL** | **$67** |

Usage-based lines (DNS queries, load balancer data, Entra workload identity) are not included in the total above.

### Infracost detection note

Infracost v0.10.44 does **not** emit line items for `azurerm_postgresql_flexible_server`, `azurerm_container_registry`, or `azurerm_log_analytics_workspace` in this stack (they are not counted in the 31 detected resources). After `terraform apply`, reconcile actual spend in Azure Cost Management or update Infracost when support is added.

## Architecture (cost-optimized)

- **Public:** Blazor and Grafana App Service (`*.azurewebsites.net`) — no Application Gateway
- **Private/internal:** AKS workloads (api-gateway ILB, Kafka, Flink, services) via private DNS
- **No private endpoints** — PostgreSQL, ACR, and Key Vault use public endpoints with Azure service defaults
- **Single VNet** — no hub-spoke peering

### Deliberately omitted (cost & privacy)

Application Gateway, WAF, private endpoints, and premium SKUs were **removed** from the demo
Terraform to keep spend within starter credits (~**$67/mo** Infracost baseline). Public App
Service URLs replace AGW as the user-facing edge; backends stay on internal AKS LoadBalancers.
See [README.md](README.md) for the full omission table and architecture diagram.

## Cost levers

| Variable | Effect |
|---|---|
| `aks_node_count` | Linear scale on largest compute line item |
| `aks_vm_size` | Node hourly rate |
| `app_service_plan_sku` | `F1` cannot run custom containers; `B1` is minimum for this app |
| `acr_sku` | `Basic` is minimum paid tier |
| `postgres_sku_name` | `B_Standard_B1ms` is minimum burstable |

## Teardown

```powershell
cd infra/terraform/environments/demo
terraform destroy
```

Bootstrap state storage is retained unless you destroy `bootstrap/`.
