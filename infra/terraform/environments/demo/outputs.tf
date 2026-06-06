output "resource_group_name" {
  value = azurerm_resource_group.main.name
}

output "dashboard_url" {
  description = "Public Blazor dashboard URL."
  value       = module.compute_appservice.blazor_url
}

output "ops_url" {
  description = "Public Grafana URL."
  value       = module.compute_appservice.grafana_url
}

output "blazor_hostname" {
  value = module.compute_appservice.blazor_default_hostname
}

output "grafana_hostname" {
  value = module.compute_appservice.grafana_default_hostname
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
  value = module.compute_appservice.blazor_name
}

output "grafana_app_name" {
  value = module.compute_appservice.grafana_name
}
