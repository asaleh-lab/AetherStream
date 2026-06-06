data "azurerm_client_config" "current" {}

resource "azurerm_resource_group" "main" {
  name     = var.resource_group_name
  location = var.location
  tags     = var.tags
}

module "networking" {
  source = "../../modules/networking"

  prefix              = var.prefix
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  tags                = var.tags
}

module "security" {
  source = "../../modules/security"

  prefix                 = var.prefix
  location               = var.location
  resource_group_name    = azurerm_resource_group.main.name
  tags                   = var.tags
  tenant_id              = data.azurerm_client_config.current.tenant_id
  terraform_principal_id = data.azurerm_client_config.current.object_id
  key_vault_name         = var.key_vault_name
}

module "data" {
  source = "../../modules/data"

  prefix                        = var.prefix
  location                      = var.location
  resource_group_name           = azurerm_resource_group.main.name
  tags                          = var.tags
  postgres_admin_password       = module.security.postgres_admin_password
  postgres_sku_name             = var.postgres_sku_name
  acr_sku                       = var.acr_sku
  log_analytics_retention_days  = var.log_analytics_retention_days
  blazor_identity_principal_id  = module.security.blazor_identity_principal_id
  grafana_identity_principal_id = module.security.grafana_identity_principal_id
  github_actions_principal_id   = var.github_actions_principal_id
}

module "compute_aks" {
  source = "../../modules/compute-aks"

  prefix              = var.prefix
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  tags                = var.tags
  aks_subnet_id       = module.networking.aks_subnet_id
  aks_node_count      = var.aks_node_count
  aks_vm_size         = var.aks_vm_size
  acr_id              = module.data.acr_id

  depends_on = [module.data]
}

resource "azurerm_role_assignment" "aks_workload_kv" {
  scope                = module.security.key_vault_id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = module.compute_aks.workload_identity_principal_id
}

resource "azurerm_private_dns_a_record" "api_gateway" {
  name                = "api-gateway"
  zone_name           = module.networking.private_dns_zone_internal_name
  resource_group_name = azurerm_resource_group.main.name
  ttl                 = 60
  records             = [var.api_gateway_ilb_ip]
}

resource "azurerm_private_dns_a_record" "prometheus" {
  name                = "prometheus"
  zone_name           = module.networking.private_dns_zone_internal_name
  resource_group_name = azurerm_resource_group.main.name
  ttl                 = 60
  records             = [var.prometheus_ilb_ip]
}

module "compute_appservice" {
  count  = var.enable_app_service ? 1 : 0
  source = "../../modules/compute-appservice"

  prefix                     = var.prefix
  location                   = var.location
  resource_group_name        = azurerm_resource_group.main.name
  tags                       = var.tags
  app_service_plan_sku       = var.app_service_plan_sku
  appsvc_subnet_id           = module.networking.appsvc_subnet_id
  acr_login_server           = module.data.acr_login_server
  blazor_identity_id         = module.security.blazor_identity_id
  blazor_identity_client_id  = module.security.blazor_identity_client_id
  grafana_identity_id        = module.security.grafana_identity_id
  grafana_identity_client_id = module.security.grafana_identity_client_id
  blazor_image               = var.blazor_image
  grafana_image              = var.grafana_image
  api_gateway_internal_host  = var.api_gateway_internal_host
  grafana_admin_password     = module.security.grafana_admin_password
  prometheus_internal_host   = var.prometheus_internal_host

  depends_on = [azurerm_private_dns_a_record.api_gateway]
}

module "observability" {
  source = "../../modules/observability"

  prefix                     = var.prefix
  location                   = var.location
  resource_group_name        = azurerm_resource_group.main.name
  tags                       = var.tags
  log_analytics_workspace_id = module.data.log_analytics_workspace_id
  app_service_ids = var.enable_app_service ? {
    blazor  = module.compute_appservice[0].blazor_id
    grafana = module.compute_appservice[0].grafana_id
  } : {}
  aks_id = module.compute_aks.aks_id
}
