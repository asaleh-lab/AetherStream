# AetherStream Azure Infrastructure (Terraform)

Single-VNet demo: **App Service B1** (Blazor + Grafana on one shared plan) as the public front,
**AKS** for the streaming backbone, lowest viable SKUs throughout. GitHub Actions CD via OIDC.

## Deliberately omitted for cost

The following are **not deployed** in this demo stack. They were cut to keep monthly spend near
**~$80/mo** and to avoid always-on edge infrastructure:

| Not deployed | Rationale |
|---|---|
| **Application Gateway** | ~$200/mo fixed — largest cost; App Service HTTPS URLs are the public front door instead |
| **WAF (Web Application Firewall)** | Tied to Application Gateway WAF_v2; no OWASP/rule-set filtering in demo |
| **Private endpoints** (UI, DB, registry, vault) | ~$7/mo each; public endpoints + RBAC/managed identity instead |
| **Hub-spoke networking** | Extra VNet, peering, and DNS complexity without benefit at demo scale |
| **Premium SKU tiers** (ACR Premium, App Service P1v3) | Required only for private-link designs |
| **Linux VM for UI** | Replaced by App Service B1 (~$13/mo) after Web quota upgrade |

**Privacy note:** Blazor and Grafana are on **public App Service URLs** (`*.azurewebsites.net`).
Streaming backends remain on internal AKS LoadBalancers (api-gateway ILB, Prometheus ILB) — not
Internet-routable. PostgreSQL, ACR, and Key Vault use **public network paths** with Azure defaults;
treat secrets and data as demo-only.

Reintroduce Application Gateway, WAF, private endpoints, and hub-spoke isolation for production.

## Architecture

```mermaid
flowchart LR
  subgraph Internet
    U([User])
  end

  subgraph RG["Resource group rg-aether-demo"]
    ASP["App Service Plan B1"]
    BLAZOR["aether-demo-blazor"]
    GRAF["aether-demo-grafana"]
    ASP --> BLAZOR
    ASP --> GRAF

    subgraph VNet["VNet 10.1.0.0/16"]
      subgraph AKS["AKS snet-aks"]
        AGW_ILB["api-gateway ILB<br/>10.1.0.10"]
        PROM_ILB["Prometheus ILB<br/>10.1.0.11"]
        PODS["Kafka · Flink · write-side<br/>· relay · datasource"]
      end
      subgraph UiNet["snet-appsvc"]
        BLAZOR
        GRAF
      end
      PDNS["Private DNS zone<br/>aether-demo.internal"]
    end

    PG[("PostgreSQL<br/>B_Standard_B1ms")]
    ACR["ACR Basic"]
    KV["Key Vault"]
    LAW["Log Analytics"]
  end

  U -->|HTTPS| BLAZOR
  U -->|HTTPS| GRAF
  BLAZOR -->|HTTP :8085| PDNS
  PDNS --> AGW_ILB
  GRAF -->|HTTP :9090| PDNS
  PDNS --> PROM_ILB
  PODS --- AGW_ILB
  AKS --> PG
  AKS --> ACR
  BLAZOR --> ACR
  GRAF --> ACR
```

**Traffic flow**

1. User opens Blazor / Grafana on App Service URLs (`dashboard_url`, `ops_url` outputs).
2. Blazor server-side calls `http://api-gateway.aether-demo.internal:8085` over the VNet.
3. Grafana reads Prometheus at `http://prometheus.aether-demo.internal:9090` the same way.
4. GitHub Actions (OIDC) builds images → ACR, deploys K8s manifests to AKS, updates App Service containers.

## Layout

```text
infra/terraform/
  bootstrap/              # One-time: remote state + GitHub OIDC app registration
  environments/demo/      # Demo environment root module
  modules/
    networking/           # VNet, subnets, internal private DNS
    security/             # Key Vault, managed identities, generated secrets
    data/                 # PostgreSQL, ACR, Log Analytics
    compute-aks/          # AKS cluster
    compute-appservice/   # Blazor + Grafana (shared B1 plan)
    observability/        # Diagnostic settings → Log Analytics
```

## Prerequisites

- Azure subscription with **Web/App Service quota** (B1 plan)
- [Terraform](https://www.terraform.io/downloads) >= 1.6
- [Azure CLI](https://learn.microsoft.com/cli/azure/install-azure-cli) logged in (`az login`)
- GitHub repository with Environments enabled (`demo`)

Register providers (once per subscription):

```powershell
az provider register --namespace Microsoft.ContainerService
az provider register --namespace Microsoft.Web
az provider register --namespace Microsoft.Network
az provider register --namespace Microsoft.DBforPostgreSQL
az provider register --namespace Microsoft.KeyVault
```

## Step 1 — Bootstrap (manual, once)

```powershell
cd infra/terraform/bootstrap
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform apply
```

Record outputs → GitHub secrets `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`.

## Step 2 — Deploy demo environment

```powershell
cd infra/terraform/environments/demo
terraform init
terraform plan
terraform apply
```

Note `dashboard_url` and `ops_url` outputs (App Service HTTPS URLs).

## Step 3 — Deploy workloads

```powershell
az aks get-credentials --resource-group rg-aether-demo --name aether-demo-aks
$pgPass = az keyvault secret show --vault-name aether4adcdemokv --name postgres-admin-password --query value -o tsv
kubectl create namespace aether --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -k infra/k8s/overlays/demo
kubectl -n aether create secret generic aether-secrets `
  --from-literal=AETHER_DB_URL="jdbc:postgresql://aether-demo-pg.postgres.database.azure.com:5432/aetherstream" `
  --from-literal=AETHER_DB_PASSWORD="$pgPass" `
  --dry-run=client -o yaml | kubectl apply -f -
kubectl -n aether rollout restart deployment/write-side deployment/outbox-relay deployment/api-gateway
```

Set GitHub Environment vars: `BLAZOR_APP_NAME=aether-demo-blazor`, `GRAFANA_APP_NAME=aether-demo-grafana`.

## Public vs private exposure

| Surface | Access |
|---|---|
| App Service (Blazor + Grafana) | Public HTTPS (`dashboard_url`, `ops_url`) |
| api-gateway, write-side, Kafka, Flink | AKS internal / ILB only |
| PostgreSQL, ACR, Key Vault | Public endpoints (demo cost model) |

## CD pipelines

- `.github/workflows/infra-cd.yml` — Terraform plan on PR, apply on `main`
- `.github/workflows/app-cd.yml` — Build/push images, deploy AKS + App Service

Both use GitHub OIDC; no long-lived Azure client secrets in the repo.

## Smoke verification

See [SMOKE-VERIFY.md](SMOKE-VERIFY.md).

## Cost notes

See [COST-ESTIMATE.md](COST-ESTIMATE.md). Application Gateway and WAF alone would add ~$200/mo;
App Service B1 adds ~$13/mo for both UI apps on one plan.

## Rollback

```powershell
cd infra/terraform/environments/demo
terraform destroy
```

Bootstrap state storage is retained unless you explicitly destroy `bootstrap/`.
