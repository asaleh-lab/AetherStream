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

resource "random_password" "postgres_admin" {
  length  = 24
  special = true
}

resource "random_password" "grafana_admin" {
  length  = 20
  special = false

  keepers = {
    # Bump to rotate after an accidental credential exposure in git history.
    rotation = "2026-06-06-v2"
  }
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
