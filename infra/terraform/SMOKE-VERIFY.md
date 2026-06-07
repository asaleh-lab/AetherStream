# Azure Demo Smoke Verification

Run after `terraform apply` and `kubectl apply` (AKS backbone + UI LoadBalancers).

**Live demo URLs and login credentials** (Blazor, Grafana) are in the **motivation letter** — use those endpoints for reviewer-facing smoke checks. They are intentionally omitted from this repository.

## 1. Network exposure

Using the Blazor and Grafana URLs from the motivation letter:

```powershell
curl -I "<blazor-url>/health"
curl -I "<grafana-url>/api/health"
```

Expected: Blazor and Grafana are publicly reachable on **AKS public LoadBalancer** URLs (HTTP).

Wait for both services to show an `EXTERNAL-IP` before testing (`kubectl get svc -n aether blazor-dashboard grafana -w`).

## 2. Blazor dashboard (live data)

1. Open the Blazor URL from the motivation letter.
2. Confirm connection badge shows gateway connected.
3. Verify pages update without refresh:
   - Energy overview cards
   - Alerts panel
   - Turbines grid
   - Recommendations page

## 3. Grafana (ops UI)

1. Open the Grafana URL from the motivation letter.
2. Log in with the credentials provided there.
3. Confirm **Loki** and **Prometheus** datasources are healthy (Configuration → Data sources).
4. Explore logs: **Explore → Loki** — try `{container="aether-datasource"}` (Last 15 minutes).
5. Explore metrics: **Explore → Prometheus** — e.g. `up{job=~"write-side|api-gateway"}`.
6. Open dashboard **AetherStream → AetherStream Logs** (same queries as local compose).

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
| Blazor unhealthy | `kubectl -n aether describe pod -l app=blazor-dashboard`; ACR image pull; `/health` on the motivation-letter URL |
| Grafana login fails | `kubectl -n aether get secret grafana-secrets`; Key Vault secret `grafana-admin-password` (synced by `app-cd`) |
| Loki empty in Grafana | `kubectl -n aether get pods -l app=loki`; `kubectl -n aether get daemonset promtail`; try `{container="aether-datasource"}` |
| No Kafka events | `kubectl -n aether logs job/kafka-init`; `kubectl -n aether get pods` |
| Flyway errors | `aether-secrets` from Key Vault (not kustomize); `kubectl get secret aether-secrets -o yaml` |
| Pods Pending (CPU) | Demo overlay resource limits; or set `aks_node_count = 2` in tfvars |
| LB stuck `<pending>` | Azure quota for public IPs / LoadBalancers; `kubectl describe svc blazor-dashboard` |
