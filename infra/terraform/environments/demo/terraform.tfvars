location                  = "northeurope"
prefix                    = "aether-demo"
resource_group_name       = "rg-aether-demo"
api_gateway_ilb_ip        = "10.1.0.10"
prometheus_ilb_ip         = "10.1.0.11"
api_gateway_internal_host = "api-gateway.aether-demo.internal"
prometheus_internal_host  = "prometheus.aether-demo.internal"

# --- Demo cost controls (see infra/terraform/COST-ESTIMATE.md) ---
aks_node_count               = 1
aks_vm_size                  = "Standard_EC2as_v5"
acr_sku                      = "Basic"
postgres_sku_name            = "B_Standard_B1ms"
log_analytics_retention_days = 30

# Set after bootstrap apply (terraform output github_actions_principal_id):
github_actions_principal_id = "e5141340-8f4d-4bbc-9ef4-2e2215f12922"
key_vault_name              = "aether4adcdemokv"
