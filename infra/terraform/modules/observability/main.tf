resource "azurerm_monitor_diagnostic_setting" "app_service" {
  for_each = var.app_service_ids

  name                       = "${var.prefix}-diag-appsvc-${each.key}"
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
