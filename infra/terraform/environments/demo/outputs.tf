output "resource_group_name" {
  value = azurerm_resource_group.main.name
}

output "dashboard_url" {
  description = "Public Blazor URL (AKS LoadBalancer). Run the command in dashboard_url_command after kubectl apply."
  value       = null
}

output "ops_url" {
  description = "Public Grafana URL (AKS LoadBalancer). Run the command in ops_url_command after kubectl apply."
  value       = null
}

output "dashboard_url_command" {
  description = "Print Blazor dashboard URL after UI pods are deployed."
  value       = "kubectl get svc blazor-dashboard -n aether -o jsonpath='http://{.status.loadBalancer.ingress[0].ip}{\"\\n\"}'"
}

output "ops_url_command" {
  description = "Print Grafana URL after UI pods are deployed."
  value       = "kubectl get svc grafana -n aether -o jsonpath='http://{.status.loadBalancer.ingress[0].ip}{\"\\n\"}'"
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
