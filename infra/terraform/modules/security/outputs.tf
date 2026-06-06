output "key_vault_id" {
  value = azurerm_key_vault.main.id
}

output "key_vault_uri" {
  value = azurerm_key_vault.main.vault_uri
}

output "key_vault_name" {
  value = azurerm_key_vault.main.name
}

output "blazor_identity_id" {
  value = azurerm_user_assigned_identity.blazor.id
}

output "blazor_identity_client_id" {
  value = azurerm_user_assigned_identity.blazor.client_id
}

output "blazor_identity_principal_id" {
  value = azurerm_user_assigned_identity.blazor.principal_id
}

output "grafana_identity_id" {
  value = azurerm_user_assigned_identity.grafana.id
}

output "grafana_identity_client_id" {
  value = azurerm_user_assigned_identity.grafana.client_id
}

output "grafana_identity_principal_id" {
  value = azurerm_user_assigned_identity.grafana.principal_id
}

output "postgres_admin_password" {
  value     = random_password.postgres_admin.result
  sensitive = true
}

output "grafana_admin_password" {
  value     = random_password.grafana_admin.result
  sensitive = true
}
