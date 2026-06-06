output "diagnostic_setting_count" {
  value = length(azurerm_monitor_diagnostic_setting.aks) > 0 ? 1 : 0
}
