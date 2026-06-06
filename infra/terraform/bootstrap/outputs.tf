output "resource_group_name" {
  description = "Bootstrap resource group name."
  value       = azurerm_resource_group.bootstrap.name
}

output "storage_account_name" {
  description = "Remote state storage account name."
  value       = azurerm_storage_account.tfstate.name
}

output "state_container_name" {
  description = "Remote state blob container name."
  value       = azurerm_storage_container.tfstate.name
}

output "github_actions_client_id" {
  description = "Azure AD application client ID for GitHub Actions OIDC."
  value       = azuread_application.github_actions.client_id
}

output "tenant_id" {
  description = "Azure AD tenant ID."
  value       = data.azurerm_client_config.current.tenant_id
}

output "subscription_id" {
  description = "Azure subscription ID."
  value       = data.azurerm_client_config.current.subscription_id
}

output "backend_config_snippet" {
  description = "Paste into environments/demo/backend.tf after bootstrap apply."
  value       = <<-EOT
    terraform {
      backend "azurerm" {
        resource_group_name  = "${azurerm_resource_group.bootstrap.name}"
        storage_account_name = "${azurerm_storage_account.tfstate.name}"
        container_name       = "${azurerm_storage_container.tfstate.name}"
        key                  = "demo.tfstate"
        use_oidc             = true
      }
    }
  EOT
}

output "github_actions_principal_id" {
  description = "Set as github_actions_principal_id in environments/demo/terraform.tfvars."
  value       = azuread_service_principal.github_actions.object_id
}

output "github_secrets" {
  description = "Configure these as GitHub repository secrets/variables."
  value = {
    AZURE_CLIENT_ID       = azuread_application.github_actions.client_id
    AZURE_TENANT_ID       = data.azurerm_client_config.current.tenant_id
    AZURE_SUBSCRIPTION_ID = data.azurerm_client_config.current.subscription_id
  }
  sensitive = false
}
