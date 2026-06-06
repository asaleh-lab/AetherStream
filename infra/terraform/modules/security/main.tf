data "azurerm_client_config" "current" {}

resource "azurerm_key_vault" "main" {
  name                          = coalesce(var.key_vault_name, replace("${var.prefix}-kv", "-", ""))
  location                      = var.location
  resource_group_name           = var.resource_group_name
  tenant_id                     = var.tenant_id
  sku_name                      = "standard"
  soft_delete_retention_days    = 7
  purge_protection_enabled      = false
  rbac_authorization_enabled    = true
  public_network_access_enabled = true
  tags                          = var.tags
}

resource "azurerm_role_assignment" "terraform_kv_secrets" {
  scope                = azurerm_key_vault.main.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = var.terraform_principal_id
}

resource "azurerm_user_assigned_identity" "blazor" {
  name                = "${var.prefix}-blazor"
  location            = var.location
  resource_group_name = var.resource_group_name
  tags                = var.tags
}

resource "azurerm_user_assigned_identity" "grafana" {
  name                = "${var.prefix}-grafana"
  location            = var.location
  resource_group_name = var.resource_group_name
  tags                = var.tags
}

resource "azurerm_role_assignment" "blazor_kv_secrets_user" {
  scope                = azurerm_key_vault.main.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_user_assigned_identity.blazor.principal_id
}

resource "azurerm_role_assignment" "grafana_kv_secrets_user" {
  scope                = azurerm_key_vault.main.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_user_assigned_identity.grafana.principal_id
}

resource "random_password" "postgres_admin" {
  length  = 24
  special = true
}

resource "random_password" "grafana_admin" {
  length  = 20
  special = false
}

resource "azurerm_key_vault_secret" "postgres_admin_password" {
  name         = "postgres-admin-password"
  value        = random_password.postgres_admin.result
  key_vault_id = azurerm_key_vault.main.id

  depends_on = [azurerm_role_assignment.terraform_kv_secrets]
}

resource "azurerm_key_vault_secret" "grafana_admin_password" {
  name         = "grafana-admin-password"
  value        = random_password.grafana_admin.result
  key_vault_id = azurerm_key_vault.main.id

  depends_on = [azurerm_role_assignment.terraform_kv_secrets]
}
