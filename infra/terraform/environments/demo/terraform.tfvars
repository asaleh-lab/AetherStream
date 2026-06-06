location             = "westeurope"
prefix               = "aether-demo"
resource_group_name  = "rg-aether-demo"
api_gateway_ilb_ip        = "10.1.0.10"
prometheus_ilb_ip         = "10.1.0.11"
api_gateway_internal_host = "api-gateway.aether-demo.internal"
prometheus_internal_host  = "prometheus.aether-demo.internal"

# --- Demo cost controls (see infra/terraform/COST-ESTIMATE.md) ---
aks_node_count               = 1
aks_vm_size                  = "Standard_B2als_v2"
app_service_plan_sku         = "B1"
acr_sku                      = "Basic"
postgres_sku_name            = "B_Standard_B1ms"
log_analytics_retention_days = 1

# Set after bootstrap apply (terraform output github_actions_principal_id):
# github_actions_principal_id = "<from-bootstrap-output>"
