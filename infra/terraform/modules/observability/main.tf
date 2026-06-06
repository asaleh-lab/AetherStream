resource "azurerm_monitor_diagnostic_setting" "app_service" {
  for_each = { for id in var.app_service_ids : id => id }

  name                       = "${var.prefix}-diag-appsvc"
  target_resource_id         = each.value
  log_analytics_workspace_id = var.log_analytics_workspace_id

  enabled_log {
    category_group = "allLogs"
  }

  metric {
    category = "AllMetrics"
    enabled  = true
  }
}

resource "azurerm_monitor_diagnostic_setting" "aks" {
  count = var.aks_id != "" ? 1 : 0

  name                       = "${var.prefix}-diag-aks"
  target_resource_id         = var.aks_id
  log_analytics_workspace_id = var.log_analytics_workspace_id

  enabled_log {
    category_group = "allLogs"
  }

  metric {
    category = "AllMetrics"
    enabled  = true
  }
}
