variable "location" {
  type    = string
  default = "northeurope"
}

variable "prefix" {
  type    = string
  default = "aether-demo"
}

variable "resource_group_name" {
  type    = string
  default = "rg-aether-demo"
}

variable "postgres_sku_name" {
  type    = string
  default = "B_Standard_B1ms"
}

variable "acr_sku" {
  type    = string
  default = "Basic"
}

variable "aks_node_count" {
  type    = number
  default = 1
}

variable "aks_vm_size" {
  type    = string
  default = "Standard_B2als_v2"
}

variable "log_analytics_retention_days" {
  type    = number
  default = 30
}

variable "api_gateway_ilb_ip" {
  description = "Static internal IP for api-gateway LoadBalancer (must be free in snet-aks)."
  type        = string
  default     = "10.1.0.10"
}

variable "prometheus_ilb_ip" {
  description = "Static internal IP for Prometheus LoadBalancer."
  type        = string
  default     = "10.1.0.11"
}

variable "api_gateway_internal_host" {
  description = "Private DNS name for api-gateway ILB (optional external consumers)."
  type        = string
  default     = "api-gateway.aether-demo.internal"
}

variable "prometheus_internal_host" {
  description = "Private DNS name for Prometheus ILB (optional external consumers)."
  type        = string
  default     = "prometheus.aether-demo.internal"
}

variable "github_actions_principal_id" {
  description = "Object ID of GitHub Actions service principal (from bootstrap)."
  type        = string
  default     = ""
}

variable "key_vault_name" {
  description = "Globally unique Key Vault name override."
  type        = string
  default     = "aether4adcdemokv"
}

variable "tags" {
  type = map(string)
  default = {
    project     = "aetherstream"
    environment = "demo"
    managed_by  = "terraform"
  }
}
