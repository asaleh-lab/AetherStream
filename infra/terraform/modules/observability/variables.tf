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

variable "log_analytics_workspace_id" {
  type = string
}

variable "app_service_ids" {
  type = map(string)
}

variable "aks_id" {
  type = string
}
