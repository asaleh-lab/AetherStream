resource "azurerm_kubernetes_cluster" "main" {
  name                = "${var.prefix}-aks"
  location            = var.location
  resource_group_name = var.resource_group_name
  dns_prefix          = "${var.prefix}-aks"
  kubernetes_version  = var.aks_kubernetes_version
  tags                = var.tags

  private_cluster_enabled           = false
  local_account_disabled            = false
  role_based_access_control_enabled = true
  workload_identity_enabled         = true
  oidc_issuer_enabled               = true

  default_node_pool {
    name                 = "system"
    vm_size              = var.aks_vm_size
    vnet_subnet_id       = var.aks_subnet_id
    node_count           = var.aks_node_count
    type                 = "VirtualMachineScaleSets"
    orchestrator_version = var.aks_kubernetes_version
    upgrade_settings {
      max_surge = "33%"
    }
  }

  identity {
    type = "SystemAssigned"
  }

  network_profile {
    network_plugin      = "azure"
    network_plugin_mode = "overlay"
    network_policy      = "azure"
    load_balancer_sku   = "standard"
    outbound_type       = "loadBalancer"
    service_cidr        = "10.2.0.0/16"
    dns_service_ip      = "10.2.0.1"
    pod_cidr            = "10.244.0.0/16"
  }

  key_vault_secrets_provider {
    secret_rotation_enabled = true
  }
}

resource "azurerm_role_assignment" "aks_acr_pull" {
  scope                = var.acr_id
  role_definition_name = "AcrPull"
  principal_id         = azurerm_kubernetes_cluster.main.kubelet_identity[0].object_id
}

resource "azurerm_user_assigned_identity" "workload" {
  name                = "${var.prefix}-aks-workload-mi"
  location            = var.location
  resource_group_name = var.resource_group_name
  tags                = var.tags
}

resource "azurerm_federated_identity_credential" "workload" {
  name                = "${var.prefix}-workload-fic"
  resource_group_name = var.resource_group_name
  audience            = ["api://AzureADTokenExchange"]
  issuer              = azurerm_kubernetes_cluster.main.oidc_issuer_url
  parent_id           = azurerm_user_assigned_identity.workload.id
  subject             = "system:serviceaccount:aether:aether-workload"
}
