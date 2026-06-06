output "vnet_id" {
  value = azurerm_virtual_network.main.id
}

output "aks_subnet_id" {
  value = azurerm_subnet.aks.id
}

output "private_dns_zone_internal_id" {
  value = azurerm_private_dns_zone.internal.id
}

output "private_dns_zone_internal_name" {
  value = azurerm_private_dns_zone.internal.name
}
