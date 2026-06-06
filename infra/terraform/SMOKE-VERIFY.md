# Azure Demo Smoke Verification

Run after `terraform apply` and `kubectl apply` (AKS backbone + UI LoadBalancers).

## 1. Network exposure

```powershell
$dashboard = kubectl get svc blazor-dashboard -n aether -o jsonpath='http://{.status.loadBalancer.ingress[0].ip}'
$ops = kubectl get svc grafana -n aether -o jsonpath='http://{.status.loadBalancer.ingress[0].ip}'

curl -I "$dashboard/health"
curl -I "$ops/api/health"
```

Expected: Blazor and Grafana are publicly reachable on **AKS public LoadBalancer** URLs (HTTP).

Wait for `EXTERNAL-IP` on both services (`kubectl get svc -n aether blazor-dashboard grafana -w`).

## 2. Blazor dashboard (live data)

1. Open `$dashboard` from above.
2. Confirm connection badge shows gateway connected.
3. Verify pages update without refresh:
   - Energy overview cards
   - Alerts panel
   - Turbines grid
   - Recommendations page

## 3. Grafana (ops UI)

1. Open `$ops` from above.
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
2. Confirm `app-cd` workflow builds, pushes `:latest` and `:sha`, and restarts AKS deployments (including Blazor + Grafana).
3. Re-run dashboard smoke checks — live data still flows.

## Troubleshooting

| Symptom | Check |
|---|---|
| Blazor shows disconnected | `kubectl -n aether logs deployment/blazor-dashboard`; api-gateway pod healthy; in-cluster DNS `api-gateway:8085` |
| Blazor unhealthy | `kubectl -n aether describe pod -l app=blazor-dashboard`; ACR image pull; `/health` on LB URL |
| Grafana login fails | `kubectl -n aether get secret grafana-secrets`; Key Vault `grafana-admin-password` |
| No Kafka events | `kubectl -n aether logs job/kafka-init`; `kubectl -n aether get pods` |
| Flyway errors | `aether-secrets` from Key Vault (not kustomize); `kubectl get secret aether-secrets -o yaml` |
| Pods Pending (CPU) | Demo overlay resource limits; or set `aks_node_count = 2` in tfvars |
| LB stuck `<pending>` | Azure quota for public IPs / LoadBalancers; `kubectl describe svc blazor-dashboard` |
