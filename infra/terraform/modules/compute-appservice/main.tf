resource "azurerm_service_plan" "main" {
  name                = "${var.prefix}-asp"
  location            = var.location
  resource_group_name = var.resource_group_name
  os_type             = "Linux"
  sku_name            = var.app_service_plan_sku
  tags                = var.tags
}

resource "azurerm_linux_web_app" "blazor" {
  name                = "${var.prefix}-blazor"
  location            = var.location
  resource_group_name = var.resource_group_name
  service_plan_id     = azurerm_service_plan.main.id
  tags                = var.tags

  public_network_access_enabled = true
  https_only                    = true

  identity {
    type         = "UserAssigned"
    identity_ids = [var.blazor_identity_id]
  }

  site_config {
    always_on                                     = true
    container_registry_use_managed_identity       = true
    container_registry_managed_identity_client_id = var.blazor_identity_client_id

    application_stack {
      docker_image_name   = var.blazor_image
      docker_registry_url = "https://${var.acr_login_server}"
    }

    health_check_path                 = "/health"
    health_check_eviction_time_in_min = 5
  }

  app_settings = {
    "Gateway__BaseUrl"                    = "http://${var.api_gateway_internal_host}:${var.api_gateway_port}"
    "Gateway__WebSocketUrl"               = "ws://${var.api_gateway_internal_host}:${var.api_gateway_port}/ws/realtime"
    "ASPNETCORE_FORWARDEDHEADERS_ENABLED" = "true"
    "Dashboard__DisplayTimeZoneId"        = "Europe/Berlin"
    "WEBSITES_PORT"                       = "8080"
    "WEBSITE_DNS_SERVER"                  = "168.63.129.16"
  }

  virtual_network_subnet_id = var.appsvc_subnet_id
}

resource "azurerm_linux_web_app" "grafana" {
  name                = "${var.prefix}-grafana"
  location            = var.location
  resource_group_name = var.resource_group_name
  service_plan_id     = azurerm_service_plan.main.id
  tags                = var.tags

  public_network_access_enabled = true
  https_only                    = true

  identity {
    type         = "UserAssigned"
    identity_ids = [var.grafana_identity_id]
  }

  site_config {
    always_on                                     = true
    container_registry_use_managed_identity       = true
    container_registry_managed_identity_client_id = var.grafana_identity_client_id

    application_stack {
      docker_image_name   = var.grafana_image
      docker_registry_url = "https://${var.acr_login_server}"
    }

    health_check_path                 = "/api/health"
    health_check_eviction_time_in_min = 5
  }

  app_settings = {
    "GF_SECURITY_ADMIN_USER"     = "admin"
    "GF_SECURITY_ADMIN_PASSWORD" = var.grafana_admin_password
    "GF_USERS_ALLOW_SIGN_UP"     = "false"
    "GF_SERVER_ROOT_URL"         = "https://${var.prefix}-grafana.azurewebsites.net"
    "PROMETHEUS_URL"             = "http://${var.prometheus_internal_host}:9090"
    "WEBSITES_PORT"              = "3000"
    "WEBSITE_DNS_SERVER"         = "168.63.129.16"
  }

  virtual_network_subnet_id = var.appsvc_subnet_id
}
