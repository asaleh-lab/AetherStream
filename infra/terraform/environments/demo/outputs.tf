output "resource_group_name" {
  value = azurerm_resource_group.main.name
}

output "dashboard_url" {
  description = "Public Blazor dashboard URL (App Service)."
  value       = var.enable_app_service ? module.compute_appservice[0].blazor_url : null
}

output "ops_url" {
  description = "Public Grafana URL (App Service)."
  value       = var.enable_app_service ? module.compute_appservice[0].grafana_url : null
}

output "blazor_hostname" {
  value = var.enable_app_service ? module.compute_appservice[0].blazor_default_hostname : null
}

output "grafana_hostname" {
  value = var.enable_app_service ? module.compute_appservice[0].grafana_default_hostname : null
}

output "acr_login_server" {
  value = module.data.acr_login_server
}

output "acr_name" {
  value = module.data.acr_name
}

output "aks_name" {
  value = module.compute_aks.aks_name
}

output "postgres_fqdn" {
  value = module.data.postgres_fqdn
}

output "postgres_database_name" {
  value = module.data.postgres_database_name
}

output "key_vault_name" {
  value = module.security.key_vault_name
}

output "api_gateway_internal_host" {
  value = var.api_gateway_internal_host
}

output "blazor_app_name" {
  value = var.enable_app_service ? module.compute_appservice[0].blazor_name : null
}

output "grafana_app_name" {
  value = var.enable_app_service ? module.compute_appservice[0].grafana_name : null
}
