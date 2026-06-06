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

variable "app_service_plan_sku" {
  type    = string
  default = "B1"
}

variable "appsvc_subnet_id" {
  type = string
}

variable "acr_login_server" {
  type = string
}

variable "blazor_identity_id" {
  type = string
}

variable "blazor_identity_client_id" {
  type = string
}

variable "grafana_identity_id" {
  type = string
}

variable "grafana_identity_client_id" {
  type = string
}

variable "blazor_image" {
  type    = string
  default = "mcr.microsoft.com/azuredocs/aci-helloworld:latest"
}

variable "grafana_image" {
  type    = string
  default = "grafana/grafana:11.0.0"
}

variable "api_gateway_internal_host" {
  description = "Private DNS host for api-gateway ILB reachable from App Service VNet integration."
  type        = string
}

variable "api_gateway_port" {
  type    = number
  default = 8085
}

variable "grafana_admin_password" {
  type      = string
  sensitive = true
}

variable "prometheus_internal_host" {
  description = "Private DNS host for Prometheus ILB reachable from App Service."
  type        = string
  default     = "prometheus.aether-demo.internal"
}
