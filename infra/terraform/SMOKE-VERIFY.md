# Azure Demo Smoke Verification

Run after `terraform apply` and workloads are deployed (AKS + App Service).

## 1. Network exposure

```powershell
cd infra/terraform/environments/demo
$dashboard = terraform output -raw dashboard_url
$ops = terraform output -raw ops_url

curl -I "$dashboard/health"
curl -I "$ops/api/health"
```

Expected: Blazor and Grafana are publicly reachable on **App Service HTTPS** URLs.

## 2. Blazor dashboard (live data)

1. Open `terraform output -raw dashboard_url`.
2. Confirm connection badge shows gateway connected.
3. Verify pages update without refresh:
   - Energy overview cards
   - Alerts panel
   - Turbines grid
   - Recommendations page

## 3. Grafana (ops UI)

1. Open `terraform output -raw ops_url`.
2. Login with admin credentials from Key Vault secret `grafana-admin-password`.
3. Confirm Prometheus datasource is healthy (Explore → Prometheus metrics).

## 4. Private backends

From a machine **without** VNet access, these must not respond on public IPs:

- `write-side`, `datasource`, `outbox-relay` HTTP ports
- Kafka broker
- api-gateway ILB (10.1.0.10) — not routable from Internet

## 5. Infrastructure drift

```powershell
cd infra/terraform/environments/demo
terraform plan
```

Expected: `No changes.`

## 6. App CD (image update without Terraform)

1. Push a trivial change to a service Dockerfile or source file.
2. Confirm `app-cd` workflow builds, pushes `:latest` and `:sha`, and restarts AKS + App Service containers.
3. Re-run dashboard smoke checks — live data still flows.

## Troubleshooting

| Symptom | Check |
|---|---|
| Blazor shows disconnected | App Service VNet integration; DNS `api-gateway.aether-demo.internal:8085`; ILB IP `10.1.0.10` |
| Blazor unhealthy | App Service logs; ACR images; `/health` on Blazor URL |
| No Kafka events | `kubectl -n aether logs job/kafka-init`; `kubectl -n aether get pods` |
| Flyway errors | `aether-secrets` from Key Vault (not kustomize); `kubectl get secret aether-secrets -o yaml` |
| Pods Pending (CPU) | Demo overlay resource limits; or set `aks_node_count = 2` in tfvars |
