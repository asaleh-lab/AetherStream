variable "prefix" {
  type = string
}

variable "location" {
  type = string
}

variable "resource_group_name" {
  type = string
}

variable "tags" {
  type = map(string)
}

variable "postgres_admin_login" {
  type    = string
  default = "aetheradmin"
}

variable "postgres_admin_password" {
  type      = string
  sensitive = true
}

variable "postgres_sku_name" {
  type    = string
  default = "B_Standard_B1ms"
}

variable "postgres_storage_mb" {
  type    = number
  default = 32768
}

variable "log_analytics_retention_days" {
  type    = number
  default = 1
}

variable "postgres_database_name" {
  type    = string
  default = "aetherstream"
}

variable "acr_sku" {
  type    = string
  default = "Basic"
}

variable "blazor_identity_principal_id" {
  type    = string
  default = ""
}

variable "grafana_identity_principal_id" {
  type    = string
  default = ""
}

variable "github_actions_principal_id" {
  type    = string
  default = ""
}
