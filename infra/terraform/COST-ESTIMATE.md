# Azure demo cost estimate

**North Europe** monthly costs for the default `terraform.tfvars` (lowest viable demo SKUs).
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
| `app_service_plan_sku` | `B1` | Minimum for custom containers; one plan hosts Blazor + Grafana |
| `aks_vm_size` | `Standard_EC2as_v5` | Subscription-allowed compute node |
| `aks_node_count` | `1` | |
| `acr_sku` | `Basic` | Public registry |
| `postgres_sku_name` | `B_Standard_B1ms` | Smallest Flexible Server burstable |
| `log_analytics_retention_days` | `30` | Azure minimum |

## Infracost breakdown

**Total: ~$67/mo** (AKS/platform baseline; add ~$13/mo App Service B1 after apply)

| Resource | Monthly cost |
|---|---:|
| AKS — 1× node + OS disk | ~$53 |
| App Service Plan — B1 | ~$13 |
| Private DNS zone (internal) | $0.50 |
| **OVERALL TOTAL (approx.)** | **~$80** |

Usage-based lines (DNS queries, load balancer data, Entra workload identity) are not included in the total above.

### Infracost detection note

Infracost v0.10.44 does **not** emit line items for `azurerm_postgresql_flexible_server`, `azurerm_container_registry`, or `azurerm_log_analytics_workspace` in this stack. After `terraform apply`, reconcile actual spend in Azure Cost Management.

## Architecture (cost-optimized)

- **Public:** App Service — Blazor + Grafana on shared B1 plan (HTTPS `*.azurewebsites.net`)
- **Private/internal:** AKS workloads (api-gateway ILB, Kafka, Flink, services) via private DNS
- **Not deployed for cost:** Application Gateway (~$200/mo), WAF, private endpoints, hub-spoke
- **Single VNet** — no hub-spoke peering

### Deliberately omitted for cost

| Omitted | Approx. saving / rationale |
|---|---|
| **Application Gateway** | ~$200/mo — App Service is the HTTPS edge |
| **WAF** | Requires AGW WAF_v2 |
| **Private endpoints** | ~$7/mo each |
| **Hub-spoke networking** | Operational complexity |
| **Premium App Service / ACR** | Only needed for private-link designs |

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
