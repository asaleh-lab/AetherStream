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
| `aks_vm_size` | `Standard_EC2as_v5` | Subscription-allowed compute node |
| `aks_node_count` | `1` | Hosts backbone + Blazor + Grafana |
| `acr_sku` | `Basic` | Public registry |
| `postgres_sku_name` | `B_Standard_B1ms` | Smallest Flexible Server burstable |
| `log_analytics_retention_days` | `30` | Azure minimum |

## Infracost breakdown

**Total: ~$67/mo** (AKS/platform baseline; add ~$18/mo per public UI LoadBalancer ≈ **~$85/mo**)

| Resource | Monthly cost |
|---|---:|
| AKS — 1× node + OS disk | ~$53 |
| Private DNS zone (internal) | $0.50 |
| Public LoadBalancers (Blazor + Grafana, est.) | ~$36 |
| **OVERALL TOTAL (approx.)** | **~$85** |

Usage-based lines (DNS queries, load balancer data, Entra workload identity) are not included in the total above.

### Infracost detection note

Infracost v0.10.44 does **not** emit line items for `azurerm_postgresql_flexible_server`, `azurerm_container_registry`, `azurerm_log_analytics_workspace`, or Kubernetes LoadBalancer services in this stack. After deploy, reconcile actual spend in Azure Cost Management.

## Architecture (cost-optimized)

- **Public:** AKS LoadBalancers — Blazor + Grafana (HTTP on public IPs)
- **Private/internal:** api-gateway ILB, Prometheus ILB, Kafka, Flink, services
- **Not deployed for cost:** Application Gateway (~$200/mo), WAF, private endpoints, hub-spoke, App Service
- **Single VNet** — no hub-spoke peering

### Deliberately omitted for cost

| Omitted | Approx. saving / rationale |
|---|---|
| **Application Gateway** | ~$200/mo — AKS LoadBalancers are the HTTP edge |
| **WAF** | Requires AGW WAF_v2 |
| **Private endpoints** | ~$7/mo each |
| **Hub-spoke networking** | Operational complexity |
| **App Service B1** | Quota blocked; UI moved to AKS instead |

## Cost levers

| Variable | Effect |
|---|---|
| `aks_node_count` | Linear scale on largest compute line item |
| `aks_vm_size` | Node hourly rate |
| `acr_sku` | `Basic` is minimum paid tier |
| `postgres_sku_name` | `B_Standard_B1ms` is minimum burstable |

## Teardown

```powershell
cd infra/terraform/environments/demo
terraform destroy
```

Bootstrap state storage is retained unless you destroy `bootstrap/`.
