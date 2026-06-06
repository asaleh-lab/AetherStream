output "diagnostic_setting_count" {
  value = length(azurerm_monitor_diagnostic_setting.app_service) + length(azurerm_monitor_diagnostic_setting.aks)
}
