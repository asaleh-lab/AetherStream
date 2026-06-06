output "blazor_id" {
  value = azurerm_linux_web_app.blazor.id
}

output "blazor_name" {
  value = azurerm_linux_web_app.blazor.name
}

output "blazor_default_hostname" {
  value = azurerm_linux_web_app.blazor.default_hostname
}

output "blazor_url" {
  value = "https://${azurerm_linux_web_app.blazor.default_hostname}"
}

output "grafana_id" {
  value = azurerm_linux_web_app.grafana.id
}

output "grafana_name" {
  value = azurerm_linux_web_app.grafana.name
}

output "grafana_default_hostname" {
  value = azurerm_linux_web_app.grafana.default_hostname
}

output "grafana_url" {
  value = "https://${azurerm_linux_web_app.grafana.default_hostname}"
}

output "service_plan_id" {
  value = azurerm_service_plan.main.id
}
